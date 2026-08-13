"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const client_1 = require("@prisma/client");
const prisma = new client_1.PrismaClient();
async function main() {
    console.log('Seeding data...');
    let artista = await prisma.artista.findFirst({ where: { nombre: 'José Alfredo Jiménez' } });
    if (!artista) {
        artista = await prisma.artista.create({
            data: {
                nombre: 'José Alfredo Jiménez',
                imagenUrl: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=800&q=80',
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
    }
    console.log('Artista y Biografía listos.');
    const bcrypt = await import('bcryptjs');
    const hashedPassAdmin = await bcrypt.hash('admin123', 10);
    let usuarioAdmin = await prisma.usuario.findUnique({ where: { correo: 'admin@admin.com' } });
    if (!usuarioAdmin) {
        usuarioAdmin = await prisma.usuario.create({
            data: {
                nombre: 'Administrador Principal',
                correo: 'admin@admin.com',
                contrasena: hashedPassAdmin,
                rol: 'ADMINISTRADOR',
            }
        });
    }
    else {
        usuarioAdmin = await prisma.usuario.update({
            where: { correo: 'admin@admin.com' },
            data: {
                contrasena: hashedPassAdmin,
                rol: 'ADMINISTRADOR'
            }
        });
    }
    let admin = await prisma.administrador.findUnique({ where: { usuarioId: usuarioAdmin.id } });
    if (!admin) {
        admin = await prisma.administrador.create({
            data: {
                usuarioId: usuarioAdmin.id,
                nivel: 1
            }
        });
    }
    console.log('Administrador admin@admin.com configurado correctamente.');
    await prisma.cancion.createMany({
        data: [
            {
                titulo: 'El Rey',
                artista: 'José Alfredo Jiménez',
                duracion: 180,
                archivoUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3',
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
    const galeria = await prisma.galeria.create({
        data: {
            nombre: 'Golden Era',
            categoria: 'GOLDEN_ERA',
            administradorId: admin.id,
            imagenes: {
                create: [
                    {
                        url: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=800&q=80',
                        titulo: 'Gala Inaugural Mariachi',
                        orden: 1
                    },
                    {
                        url: 'https://images.unsplash.com/photo-1465847899084-d164df4dedc6?auto=format&fit=crop&w=800&q=80',
                        titulo: 'Concierto en Dolores Hidalgo',
                        orden: 2
                    },
                    {
                        url: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=800&q=80',
                        titulo: 'Noche de Homenaje',
                        orden: 3
                    },
                    {
                        url: 'https://images.unsplash.com/photo-1501386761578-eac5c94b800a?auto=format&fit=crop&w=800&q=80',
                        titulo: 'Escenario Principal',
                        orden: 4
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
//# sourceMappingURL=seed.js.map