const { PrismaClient } = require('@prisma/client');
const p = new PrismaClient();
p.evento.findMany({ select: { id: true, nombre: true } })
  .then(r => console.log(JSON.stringify(r)))
  .catch(e => console.error(e))
  .finally(() => p.$disconnect());
