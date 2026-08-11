import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

async function main() {
  console.log('Seeding data...');

  // 1. Artista y Biografía
  const artista = await prisma.artista.create({
    data: {
      nombre: 'José Alfredo Jiménez',
      imagenUrl: 'https://upload.wikimedia.org/wikipedia/commons/e/ee/Jos%C3%A9_Alfredo_Jim%C3%A9nez.jpg',
      biografia: {
        create: {
          descripcion: 'José Alfredo Jiménez Sandoval fue un cantante y compositor mexicano, considerado uno de los más grandes exponentes de la música regional mexicana.',
          citaCelebre: '"El dinero no vale nada"',
          hitos: JSON.stringify(['Nació en Dolores Hidalgo', 'Escribió más de mil canciones']),
          discografia: JSON.stringify(['El Rey', 'Caminos de Guanajuato'])
        }
      }
    }
  });
  console.log('Artista y Biografía creados.');

  // Necesitamos un administrador para relacionar la galería y las canciones
  // Vamos a crear un admin dummy o buscar uno
  const usuarioAdmin = await prisma.usuario.create({
    data: {
      nombre: 'Admin Seed',
      correo: 'adminseed@festivaltrack.com',
      contrasena: 'password', // hash en prod
      rol: 'ADMINISTRADOR',
    }
  });

  const admin = await prisma.administrador.create({
    data: {
      usuarioId: usuarioAdmin.id,
      nivel: 1
    }
  });

  // 2. Canciones
  await prisma.cancion.createMany({
    data: [
      {
        titulo: 'El Rey',
        artista: 'José Alfredo Jiménez',
        duracion: 180, // Segundos
        archivoUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3', // MP3 público de prueba
        genero: 'Ranchera',
        estado: 'PUBLICADA',
        administradorId: admin.id
      },
      {
        titulo: 'Caminos de Guanajuato',
        artista: 'José Alfredo Jiménez',
        duracion: 210,
        archivoUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3',
        genero: 'Ranchera',
        estado: 'PUBLICADA',
        administradorId: admin.id
      }
    ]
  });
  console.log('Canciones creadas.');

  // 3. Galería
  const galeria = await prisma.galeria.create({
    data: {
      nombre: 'Golden Era',
      categoria: 'GOLDEN_ERA',
      administradorId: admin.id,
      imagenes: {
        create: [
          {
            url: 'https://upload.wikimedia.org/wikipedia/commons/e/ee/Jos%C3%A9_Alfredo_Jim%C3%A9nez.jpg',
            titulo: 'Retrato Clásico',
            orden: 1
          },
          {
            url: 'https://upload.wikimedia.org/wikipedia/commons/7/77/Jose_Alfredo_Jimenez.jpg',
            titulo: 'En Concierto',
            orden: 2
          }
        ]
      }
    }
  });
  console.log('Galería creada.');

  console.log('Seed terminado.');
}

main()
  .catch(e => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
