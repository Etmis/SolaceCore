import bcrypt from 'bcryptjs'

// Tento skript generuje hash hesla pro použití v databázi
// Použití: node server/generate-password.js <heslo>

const password = process.argv[2]

if (!password) {
  console.error('Usage: node server/generate-password.js <password>')
  process.exit(1)
}

const salt = bcrypt.genSaltSync(10)
const hash = bcrypt.hashSync(password, salt)

console.log('\nPassword Hash Generated:')
console.log('------------------------')
console.log('Password:', password)
console.log('Hash:', hash)
console.log('\nSQL pro vložení moderátora:')
console.log(`INSERT INTO moderators (username, password_hash) VALUES ('your_username', '${hash}');`)
console.log('')
