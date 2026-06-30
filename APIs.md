# 🔌 Integraciones de Terceros (APIs)

En este proyecto, consumimos servicios externos para habilitar la funcionalidad de mapas y geolocalización en tiempo real dentro de la aplicación de reloj inteligente (Wear OS).

---

## 🗺️ 1. Open Source Routing Machine (OSRM)

### ¿Qué API se utiliza?
Se utiliza la API pública de enrutamiento de **Project OSRM (Open Source Routing Machine)**, el cual es un motor de cálculo de rutas de alto rendimiento basado en datos de OpenStreetMap.

### ¿Para qué se utiliza?
El propósito de negocio es guiar a los asistentes del festival de manera exacta. La API resuelve el problema de trazar una ruta peatonal (caminando) desde la ubicación actual del usuario (GPS del reloj) hasta la ubicación exacta donde se llevará a cabo un evento (ej. el Mausoleo de José Alfredo). Además, nos permite calcular la **distancia en metros/kilómetros** y el **tiempo estimado de llegada**.

### ¿Cómo se utiliza?
- **Autenticación:** Es una API REST pública y gratuita; no requiere llaves de autenticación (`API_KEYS`) bajo los límites de uso justo.
- **Endpoint principal consumido:** 
  `GET https://router.project-osrm.org/route/v1/foot/{lon_origen},{lat_origen};{lon_destino},{lat_destino}?geometries=geojson`
- **Flujo de la información:**
  1. La aplicación Wear OS detecta las coordenadas actuales del usuario usando `FusedLocationProviderClient` de Android.
  2. Se extraen las coordenadas de destino desde el evento guardado en la base de datos local (Room).
  3. Se realiza la petición HTTP `GET` pasándole ambas coordenadas al endpoint de OSRM (modalidad *foot* / peatonal).
  4. OSRM responde con un objeto `JSON` que contiene un arreglo llamado `routes`.
  5. La aplicación lee `duration`, `distance`, y extrae el arreglo de `coordinates` (GeoJSON) para dibujar una Polyline (línea de guía) visualmente sobre el mapa de OSMDroid.

---

## 🌍 2. OSMDroid / Mapnik (Tile Server API)

### ¿Qué API se utiliza?
Se utiliza la API de Web Map Service (WMS) a través de los servidores de mosaicos (Tile Servers) públicos de **Mapnik** (OpenStreetMap).

### ¿Para qué se utiliza?
Provee las imágenes visuales (el dibujo de las calles, parques y bloques) que conforman el mapa gráfico que ve el usuario en la pantalla del reloj. 

### ¿Cómo se utiliza?
- **Autenticación:** API pública. Se debe incluir un encabezado HTTP `User-Agent` único (`FestivalTrack/1.0`) para cumplir con las políticas de uso de OSM.
- **Flujo de la información:**
  La librería `osmdroid` solicita automáticamente de manera asíncrona imágenes `.png` al servidor basándose en las coordenadas (X, Y) y el nivel de zoom de la cámara de la pantalla. Estas imágenes se renderizan de fondo en el lienzo debajo de la ruta calculada por OSRM.

### Imagen de como funciona la API
<img width="420" height="413" alt="image" src="https://github.com/user-attachments/assets/5b89bd74-014a-45c2-92b7-a6c53c4a07e0" />

### Formato Json que arroja
<img width="1866" height="487" alt="image" src="https://github.com/user-attachments/assets/54634f92-0b1b-4beb-a790-0f2d42091c39" />

