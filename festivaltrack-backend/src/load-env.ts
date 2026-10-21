// Debe ser el PRIMER import de main.ts: algunos módulos (p. ej. JwtModule.register)
// leen process.env al importarse, así que el .env tiene que cargarse antes que ellos.
try {
  process.loadEnvFile();
} catch {
  /* sin .env: se usan las variables del sistema */
}
