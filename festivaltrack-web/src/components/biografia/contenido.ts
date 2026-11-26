/** Contenido fijo que el móvil lleva en su código (BiographyScreen.kt y BiographyFirestoreService.kt). */

const BASE = '/images/biografia/';

export const CITA_FIJA = 'No tengo trono ni reina, ni nadie que me comprenda, pero sigo siendo el Rey.';

export const RESUMEN_FIJO =
  'José Alfredo Jiménez Sandoval (Dolores Hidalgo, 1926 – Ciudad de México, 1973) es el compositor más trascendental de la música vernácula mexicana. Sus canciones capturaron el corazón, el despecho, el amor y la bravura de todo un pueblo sin haber cursado estudios formales de música.';

export const RETRATO_LOCAL = `${BASE}jose_alfredo_portrait.webp`;

export interface Album {
  titulo: string;
  subtitulo: string;
  imagen: string;
}

export const ALBUMES: readonly Album[] = [
  { titulo: 'La Enorme Distancia', subtitulo: '1950 • 12 Canciones', imagen: `${BASE}ranchera_guitar.webp` },
  { titulo: 'El Camino de la Noche', subtitulo: '1954 • 10 Canciones', imagen: `${BASE}mariachi_gala_stage.webp` },
  { titulo: 'El Rey (Edición Oro)', subtitulo: '1968 • 14 Canciones', imagen: `${BASE}jose_alfredo_portrait.webp` },
  { titulo: 'Caminos de Guanajuato', subtitulo: '1953 • 12 Canciones', imagen: `${BASE}hero_dolores_hidalgo.webp` },
];

export interface EntradaArchivo {
  id: string;
  titulo: string;
  epoca: string;
  ubicacion: string;
  descripcion: string;
  cita: string;
  imagen: string;
  alt: string;
}

export const ARCHIVO_HISTORICO: readonly EntradaArchivo[] = [
  {
    id: 'sombrero-charro',
    titulo: 'El Sombrero Charro y la Mirada Inmortal',
    epoca: '1954',
    ubicacion: 'Dolores Hidalgo, Guanajuato',
    descripcion:
      'Fotografía icónica de estudio con su emblemático traje de charro y sombrero bordado en hilo de plata. Representa la cumbre de su elegancia artística y el orgullo de representar a Guanajuato en el mundo.',
    cita: '"No tengo trono ni reina, ni nadie que me comprenda, pero sigo siendo el Rey."',
    imagen: `${BASE}jose_alfredo_portrait.webp`,
    alt: 'José Alfredo Jiménez con traje de charro y sombrero bordado',
  },
  {
    id: 'nacimiento',
    titulo: 'El Nacimiento en la Cuna de la Independencia',
    epoca: '1926',
    ubicacion: 'Dolores Hidalgo, Gto.',
    descripcion:
      'Nace el 19 de enero en Dolores Hidalgo, Guanajuato. Hijo de Agustín Jiménez y Carmen Sandoval. Creció entre los portales coloniales y la plaza histórica, empapándose de la poesía popular y el folclor guanajuatense.',
    cita: '"Nací en el pueblo de Dolores Hidalgo, donde los hombres saben amar y morir cantando."',
    imagen: `${BASE}gal_plaza_dolores.webp`,
    alt: 'Plaza histórica de Dolores Hidalgo',
  },
  {
    id: 'gala-mariachi',
    titulo: 'La Gran Gala y Serenata del Mariachi',
    epoca: '1958',
    ubicacion: 'Plaza Principal de Dolores Hidalgo',
    descripcion:
      'Presentación estelar acompañado del Mariachi Vargas de Tecalitlán ante más de diez mil personas. Sus arreglos con trompetas y guitarras revolucionaron la música vernácula mexicana.',
    cita: '"Si nos dejan, nos vamos a querer toda la vida."',
    imagen: `${BASE}mariachi_gala_stage.webp`,
    alt: 'Escenario de gala con mariachi',
  },
  {
    id: 'yo-1948',
    titulo: 'Primer Gran Éxito: Yo (1948)',
    epoca: '1948',
    ubicacion: 'Estudios XEW, Ciudad de México',
    descripcion:
      "El afamado trío de Andrés Huesca y sus Costeños graba su composición 'Yo'. Se convierte en un himno nacional de inmediato y abre las puertas de la prestigiosa estación XEW 'La Voz de la América Latina'.",
    cita: '"Yo me fui por una copa, y me tomé la botella entera..."',
    imagen: `${BASE}gal_estudio_xew.webp`,
    alt: 'Estudio de grabación de la XEW',
  },
  {
    id: 'caminos-guanajuato',
    titulo: 'Caminos de Guanajuato y la Huella Minera',
    epoca: '1953',
    ubicacion: 'Carretera Dolores Hidalgo - Guanajuato',
    descripcion:
      "Tras el fallecimiento de su hermano Ignacio en la refinería de Salamanca, José Alfredo compone 'Caminos de Guanajuato', un homenaje inmortal que recorre León, San Miguel de Allende, Celaya y Dolores Hidalgo.",
    cita: '"No pases por Salamanca, que allí me hiere el recuerdo..."',
    imagen: `${BASE}gal_caminos_guanajuato.webp`,
    alt: 'Camino entre cerros de Guanajuato',
  },
  {
    id: 'servilleta',
    titulo: 'El Músico que no Sabía Leer Partituras',
    epoca: 'Anécdota Legendaria',
    ubicacion: 'Restaurante La Sirena, Santa María la Ribera',
    descripcion:
      "A pesar de componer más de 300 canciones inmortales, José Alfredo jamás aprendió notación musical ni a tocar instrumentos profesionalmente. Creaba las melodías silbando e improvisando letras en servilletas de restaurantes y cantinas como 'El Salón París'.",
    cita: '"Las canciones no se componen con notas, se componen con el alma y con dolor."',
    imagen: `${BASE}gal_servilleta_cantina.webp`,
    alt: 'Mesa de cantina con una servilleta escrita a mano',
  },
  {
    id: 'el-rey',
    titulo: 'El Rey: Consagración Máxima',
    epoca: '1968',
    ubicacion: 'Dolores Hidalgo, Gto.',
    descripcion:
      "Escribe 'El Rey', considerada por la crítica internacional como la ranchera más famosa del siglo XX. Grabada por cientos de leyendas desde Vicente Fernández hasta Plácido Domingo.",
    cita: '"Con dinero y sin dinero, hago siempre lo que quiero, y mi palabra es la ley."',
    imagen: `${BASE}gal_el_rey_escenario.webp`,
    alt: 'Escenario iluminado durante la interpretación de El Rey',
  },
  {
    id: 'mausoleo',
    titulo: 'El Mausoleo de la Inmortalidad: El Sarape de Azulejos',
    epoca: '1973 - Presente',
    ubicacion: 'Mausoleo Municipal, Dolores Hidalgo',
    descripcion:
      'En el panteón municipal de Dolores Hidalgo descansa el cuerpo de José Alfredo. Su tumba es una monumental obra de arte diseñada por el arquitecto Javier Senosiain, que representa un gigantesco sarape multicolor ondeando y un gran sombrero charro de bronce.',
    cita: '"La vida no vale nada, comienza siempre llorando y así llorando se acaba."',
    imagen: `${BASE}gal_mausoleo_sarape.webp`,
    alt: 'Mausoleo en forma de sarape de azulejos',
  },
  {
    id: 'casa-museo',
    titulo: 'La Casa Museo José Alfredo Jiménez',
    epoca: 'Patrimonio vivo',
    ubicacion: 'Centro Histórico, Dolores Hidalgo',
    descripcion:
      'Una casona colonial de Dolores Hidalgo resguarda objetos personales, fotografías, trajes de charro y manuscritos del compositor. Recorrer sus salas es asomarse a la vida cotidiana del Rey de la canción ranchera.',
    cita: '"Mi casa es donde está la canción."',
    imagen: `${BASE}gal_casa_museo.webp`,
    alt: 'Fachada de la Casa Museo',
  },
];
