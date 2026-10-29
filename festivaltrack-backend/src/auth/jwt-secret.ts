/**
 * Devuelve la clave para firmar/verificar JWT desde `JWT_SECRET`.
 * Falla al arrancar si no está definida: un valor por defecto permitiría
 * a cualquiera falsificar tokens válidos.
 */
export function getJwtSecret(): string {
  const secret = process.env.JWT_SECRET;
  if (!secret) {
    throw new Error('JWT_SECRET no está definida. Configúrala en el .env o en las variables de Railway.');
  }
  return secret;
}
