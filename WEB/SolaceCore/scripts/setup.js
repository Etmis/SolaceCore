import 'dotenv/config'
import mysql from 'mysql2/promise'
import readline from 'readline'
import crypto from 'crypto'
import fs from 'fs/promises'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const projectRoot = path.join(__dirname, '..')
const envPath = path.join(projectRoot, '.env')
const envExamplePath = path.join(projectRoot, '.env.example')

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
})

function question(prompt) {
  return new Promise(resolve => {
    rl.question(prompt, resolve)
  })
}

async function main() {
  console.log('\n🚀 SolaceCore Setup\n')

  // Check if .env exists
  let envExists = false
  try {
    await fs.access(envPath)
    envExists = true
  } catch {
    // .env doesn't exist
  }

  if (!envExists) {
    console.log('📋 .env file not found. Creating from .env.example...\n')
    const envExample = await fs.readFile(envExamplePath, 'utf-8')
    await fs.writeFile(envPath, envExample)
  }

  // Load env config
  let env = {}
  try {
    const envContent = await fs.readFile(envPath, 'utf-8')
    envContent.split('\n').forEach(line => {
      const match = line.match(/^([^#][^=]*)=(.*)$/)
      if (match) {
        env[match[1].trim()] = match[2].trim()
      }
    })
  } catch (e) {
    console.error('❌ Error reading .env:', e.message)
    rl.close()
    process.exit(1)
  }

  console.log('📝 Configuration\n')

  // Database config
  const dbHost = await question(`Database host [${env.DB_HOST || '127.0.0.1'}]: `)
  env.DB_HOST = dbHost || env.DB_HOST || '127.0.0.1'

  const dbPort = await question(`Database port [${env.DB_PORT || '3306'}]: `)
  env.DB_PORT = dbPort || env.DB_PORT || '3306'

  const dbUser = await question(`Database user [${env.DB_USER || 'root'}]: `)
  env.DB_USER = dbUser || env.DB_USER || 'root'

  const dbPassword = await question(`Database password [${env.DB_PASSWORD ? '(set)' : '(empty)'}]: `)
  if (dbPassword !== '' || !env.DB_PASSWORD) {
    env.DB_PASSWORD = dbPassword
  }

  const dbName = await question(`Database name [${env.DB_NAME || 'solacecore'}]: `)
  env.DB_NAME = dbName || env.DB_NAME || 'solacecore'

  // Minecraft WebSocket config
  console.log('\n🎮 Minecraft Plugin\n')
  const wsHost = await question(`WebSocket host [${env.WS_HOST || '127.0.0.1'}]: `)
  env.WS_HOST = wsHost || env.WS_HOST || '127.0.0.1'

  const wsPort = await question(`WebSocket port [${env.WS_PORT || '40394'}]: `)
  env.WS_PORT = wsPort || env.WS_PORT || '40394'

  // Generate JWT secret if needed
  console.log('\n🔐 Security\n')
  if (!env.JWT_SECRET || env.JWT_SECRET.includes('change-me')) {
    const generateSecret = await question('Generate new JWT secret? (y/n) [y]: ')
    if (generateSecret !== 'n') {
      env.JWT_SECRET = crypto.randomBytes(64).toString('hex')
      console.log(`✓ Generated JWT secret`)
    }
  }

  // Test database connection
  console.log('\n🔗 Testing database connection...')
  try {
    const connection = await mysql.createConnection({
      host: env.DB_HOST,
      port: parseInt(env.DB_PORT),
      user: env.DB_USER,
      password: env.DB_PASSWORD || undefined
    })

    // Try to create/select database
    try {
      await connection.query(`CREATE DATABASE IF NOT EXISTS \`${env.DB_NAME}\``)
    } catch (e) {
      console.warn(`⚠️  Could not create database (might already exist): ${e.message}`)
    }

    // Check if moderator schema exists (not just any tables)
    const [schemaTables] = await connection.query(
      `SELECT COUNT(*) as count FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME IN ('moderators', 'roles')`,
      [env.DB_NAME]
    )

    if (schemaTables[0].count < 2) {
      console.log('📦 Initializing moderator system schema...')
      await connection.query(`USE \`${env.DB_NAME}\``)

      const schemaPath = path.join(projectRoot, 'server', 'schema-moderators.sql')
      const schema = await fs.readFile(schemaPath, 'utf-8')

      // Split and execute each statement
      const statements = schema.split(';').filter(s => s.trim())
      for (const stmt of statements) {
        try {
          await connection.query(stmt)
        } catch (e) {
          if (!e.message.includes('already exists')) {
            console.warn(`⚠️  ${e.message}`)
          }
        }
      }

      console.log('✓ Moderator system initialized')

      // Create first admin account from values entered by the user
      console.log('\n👤 First Administrator Account\n')
      const adminUsernameInput = await question('Admin username [admin]: ')
      const adminPass = await question('Admin password: ')
      const adminUsername = adminUsernameInput || 'admin' // Rovnou přiřadíme default

      if (adminPass) { // Kontrolujeme už jen heslo
        const bcrypt = (await import('bcryptjs')).default
        const hash = await bcrypt.hash(adminPass, 10)
        const [adminRole] = await connection.query(
          'SELECT id FROM roles WHERE name = ? LIMIT 1',
          ['Admin']
        )
        const adminRoleId = Array.isArray(adminRole) && adminRole.length > 0 ? adminRole[0].id : 1

        try {
          await connection.query(
            'INSERT INTO moderators (username, password_hash, is_active, roles) VALUES (?, ?, ?, ?)',
            [adminUsername, hash, true, JSON.stringify([adminRoleId])]
          )
          console.log(`✓ Admin account created: ${adminUsername}`)
        } catch (e) {
          if (e.message.includes('Duplicate')) {
            console.log(`⚠️  Admin account already exists`)
          } else {
            console.error(`❌ Error creating admin account: ${e.message}`)
          }
        }
      } else {
        console.log('⚠️  Admin password is required. No account created.')
      }
    } else {
      console.log(`✓ Moderator system already initialized`)

      // Check if there are any admin accounts
      await connection.query(`USE \`${env.DB_NAME}\``)
      const [existingMods] = await connection.query(
        'SELECT COUNT(*) as count FROM moderators'
      )

      if (existingMods[0].count === 0) {
        console.log('\n👤 First Administrator Account\n')
        const adminUsername = await question('Admin username [admin]: ')
        const adminPass = await question('Admin password: ')

        if (adminUsername && adminPass) {
          const bcrypt = (await import('bcryptjs')).default
          const hash = await bcrypt.hash(adminPass, 10)
          const [adminRole] = await connection.query(
            'SELECT id FROM roles WHERE name = ? LIMIT 1',
            ['Admin']
          )
          const adminRoleId = Array.isArray(adminRole) && adminRole.length > 0 ? adminRole[0].id : 1

          try {
            await connection.query(
              'INSERT INTO moderators (username, password_hash, is_active, roles) VALUES (?, ?, ?, ?)',
              [adminUsername || 'admin', hash, true, JSON.stringify([adminRoleId])]
            )
            console.log(`✓ Admin account created: ${adminUsername || 'admin'}`)
          } catch (e) {
            if (e.message.includes('Duplicate')) {
              console.log(`⚠️  Admin account already exists`)
            } else {
              console.error(`❌ Error creating admin account: ${e.message}`)
            }
          }
        } else if (!adminPass) {
          console.log('⚠️  Admin password is required. No account created.')
        }
      } else {
        console.log(`ℹ️  Already have ${existingMods[0].count} moderator(s) in database`)
      }
    }

    await connection.end()
  } catch (e) {
    console.error(`❌ Database connection failed: ${e.message}`)
    console.error('   Please check your database configuration and try again.')
    rl.close()
    process.exit(1)
  }

  // Save .env
  console.log('\n💾 Saving configuration...')
  const envContent = Object.entries(env)
    .map(([key, value]) => `${key}=${value}`)
    .join('\n')

  await fs.writeFile(envPath, envContent + '\n')
  console.log('✓ .env saved')

  console.log('\n✅ Setup complete!\n')
  console.log('Next steps:')
  console.log('  npm run build    # Build frontend')
  console.log('  npm start        # Start the application\n')

  rl.close()
}

main().catch(err => {
  console.error('❌ Setup failed:', err.message)
  rl.close()
  process.exit(1)
})
