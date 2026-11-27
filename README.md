# Kata 5: balanceo de carga dinámico con NGINX y Consul
Autora: Sofía Rivero Perea

## 1. Objetivo de la entrega
He creado un servicio de números primos y lo he organizado en dos grupos de réplicas. Uso NGINX para recibir las peticiones y repartirlas, y Consul para registrar las réplicas y comprobar cuáles están disponibles. Con el cliente puedo ver qué réplica responde y cómo cambia el reparto al detener o añadir una.
En la versión final calculo los primeros n primos de la forma 4k + 3. Para n = 8, la respuesta es [3,7,11,19,23,31,43,47].

## 2. Cómo compilar y ejecutar
Abro la carpeta raíz en IntelliJ IDEA como proyecto Maven, configuro JDK 21 y cargo las dependencias de los archivos pom.xml. En el panel Maven selecciono kata5 y ejecuto Lifecycle / clean y después package.
Con Docker Desktop funcionando, preparo una configuración Docker Compose con compose.yaml. Selecciono consul, nginx, primes-v1-a, primes-v1-b, primes-v2-a y primes-v2-b, con Build / Always y Attach to / None. La réplica primes-v1-c pertenece al perfil extra y la incorporo más adelante.
Espero a que los dos servicios tengan dos réplicas sanas en [Consul](http://localhost:8500/ui/) y ejecuto el Main de primes-client/src/main/java/kata5/Main.java. Las respuestas aparecen en la consola Run. La URL base del cliente está en la constante BASE_URL y apunta a http://localhost:8080.
Trabajo desde IntelliJ y Docker Desktop, pero dejo también los comandos equivalentes para reproducir el arranque desde la raíz del proyecto:

```shell
mvn clean package
docker compose up -d --build
```
El comando para ejecutar el cliente está en el apartado 6.1. El servicio arranca dentro de los contenedores con los argumentos definidos en Compose. Para la primera construcción se necesita conexión a Internet para descargar las dependencias y las imágenes.

## 3. Dependencias y versión de JDK
| Tecnología | Versión | Uso |
| --- | --- | --- |
| JDK | 21 | Compilar y ejecutar el proyecto. |
| Maven | 3.9.11 en la imagen de compilación | Construir el proyecto y gestionar dependencias. |
| Javalin | 7.2.4 | Publicar el servicio HTTP. |
| Gson | 2.14.0 | Generar las respuestas JSON y el registro en Consul. |
| SLF4J Simple | 2.0.20 | Mostrar los mensajes de registro. |
| Docker Desktop | 4.99.0, con Compose v2 | Ejecutar los contenedores. |
| NGINX | 1.28.0 | Enrutar y repartir las peticiones. |
| Consul | 1.21.5 | Registrar las réplicas y ofrecer el descubrimiento DNS SRV. |
Para las peticiones del cliente he utilizado HttpClient, incluido en Java.

## 4. Estructura y clases principales
El proyecto esta separado en dos módulos: primes-service y primes-client. Las clases Java están en src/main/java/kata5 dentro de cada módulo.
| Archivo | Función |
| --- | --- |
| primes-service: Webservice.java | Atiende las peticiones, calcula la serie y registra la réplica en Consul. |
| primes-service: Main.java | Arranca el servicio con sus argumentos. |
| primes-client: Main.java | Consulta las dos rutas y muestra el estado HTTP, X-Node y la respuesta. |
| primes-service/Dockerfile | Construye la imagen del servicio. |
| load-balancer/webservice.conf | Define los upstreams y las rutas de NGINX. |
| compose.yaml | Define los componentes y las réplicas del despliegue. |

## 5. Puertos y topología de red
| Componente | Puerto dentro de Docker | Acceso desde mi equipo |
| --- | --- | --- |
| NGINX | 80 | http://localhost:8080 |
| Consul, API e interfaz web | 8500 | http://localhost:8500/ui/ |
| Consul, DNS | 8600, UDP/TCP | Dentro de la red de Docker. |
| Cada réplica del servicio | 8080 | A través de NGINX. |

Todos los contenedores comparten la red kata5-primes_default y se encuentran por sus nombres de servicio. Ejecuto el cliente en mi equipo y envío sus peticiones a NGINX. Este consulta el DNS de Consul y las reparte entre las réplicas del grupo correspondiente. Consul comprueba la ruta /health de cada réplica. En NGINX uso zone, resolver y service con resolve para actualizar los destinos por DNS SRV.

## 6. Rutas y comprobación del reparto, la baja y el alta
| Ruta pública | Upstream | Servicio en Consul | Réplicas iniciales |
| --- | --- | --- | --- |
| /api/v1/primes/ | primes_v1 | primes-v1 | v1-a, v1-b |
| /api/v2/primes/ | primes_v2 | primes-v2 | v2-a, v2-b |

Las dos rutas utilizan el mismo cálculo, pero cada una tiene su propio grupo de réplicas. Con el parámetro n se indica cuántos términos se pueden obtener, entre 0 y 1000.

### 6.1. Reparto inicial
El cliente ejecutándose y reviso X-Node para identificar las réplicas que responden. Espero que aparezcan los dos nodos de cada grupo, con HTTP 200 y el mismo resultado para una misma consulta.
El comando equivalente para ejecutar el cliente es:
```shell
java -jar primes-client/target/primes-client-1.0.jar
```

### 6.2. Baja de una réplica
En Docker Desktop pulso Stop en primes-v1-a. También puedo hacerlo con:
```shell
docker compose stop primes-v1-a
```

Espero a que Consul detecte la baja y NGINX actualice sus destinos. Después compruebo que la primera ruta siga respondiendo mediante v1-b y que la segunda mantenga sus propias réplicas.

### 6.3. Alta de una réplica
En IntelliJ uso la configuración Alta-v1-c con compose.yaml, el servicio primes-v1-c, Start / Selected services, Attach to / None y Build / Always. Su comando equivalente es:
```shell
docker compose up -d --build --no-deps primes-v1-c
```
Cuando la nueva réplica está sana en Consul, compruebo que v1-c aparezca en las respuestas de la primera ruta. Durante la baja y el alta mantengo NGINX y el cliente en ejecución, sin cambiar las URL. Dejo pasar el tiempo necesario para que se actualicen los chequeos de salud y el DNS.

## 7. Vídeo explicativo
[Ver vídeo explicativo](https://youtu.be/jrDfTxbz5R0)

## 8. Repeticiones de la kata
He repetido el despliegue tres veces, manteniendo los mismos upstreams, rutas y nombres en Consul. En cada repetición he cambiado la serie calculada. La versión final está en master.
| Repetición | Rama | Variación |
| --- | --- | --- |
| R1 | develop | Primeros números primos. |
| R2 | develop_two | Primer miembro de cada pareja de primos gemelos (p, p+2). |
| R3 | develop_third | Primeros primos de la forma 4k + 3. |

## 9. Evidencias de verificación y pruebas
Mi comprobación manual consiste en revisar lo siguiente:
- Que la compilación termine correctamente y el cliente reciba el resultado esperado para n = 8.
- Que ambas rutas repartan las peticiones entre los nodos de su grupo.
- Que al detener v1-a siga respondiendo v1-b y la otra ruta continúe funcionando.
- Que al incorporar v1-c empiece a recibir peticiones sin reiniciar NGINX.
