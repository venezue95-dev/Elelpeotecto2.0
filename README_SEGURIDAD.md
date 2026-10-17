# Seguridad de toDus Download

## Qué protege la app ahora
- **R8 activado** (release): nombres de clases y métodos ilegibles, código sin usar eliminado, logs borrados.
- **Valores de red ofuscados** (`Obf.kt`): la IP, el host y el User-Agent no aparecen como texto en el APK.
- **VPN bloqueada**: no se descarga mientras haya una VPN activa. HTTP Canary, PCAPdroid y similares funcionan como VPN local, así que quedan bloqueados (`NetworkGuard.kt`, se controla con `TodusConfig.BLOCK_VPN`).
- **Proxy del sistema ignorado**: el cliente usa `Proxy.NO_PROXY`; un proxy de Wi-Fi (Charles, mitmproxy) no recibe el tráfico.
- **Certificado fijado (opcional, ver abajo)**: con la huella configurada, ninguna herramienta puede leer la comunicación aunque instale su propio certificado.

## Fijar el certificado (muy recomendable)
Sin este paso la app acepta cualquier certificado de los hosts de toDus, así que una herramienta que consiga
interceptar la conexión podría leerla. Para activarlo, desde un ordenador con acceso a la red nacional:

```bash
openssl s_client -connect <IP_NACIONAL>:443 -servername s3.todus.cu </dev/null 2>/dev/null \
  | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform der \
  | openssl dgst -sha256 -binary | base64
```

Pega el resultado en `TodusConfig.PIN_SHA256`:

```kotlin
val PIN_SHA256: List<String> = listOf("AbCdEf...=")
```

Si el servidor cambia de certificado, hay que actualizar la huella y publicar una versión nueva
(pon dos huellas, la actual y la siguiente, para poder rotar sin romper la app).

## Lo que ninguna técnica garantiza
**No existe protección al 100 %.** El APK se ejecuta en un teléfono que controla otra persona:
- Quien tenga el teléfono con root, o use Frida/Xposed, puede leer los valores en memoria cuando la app los usa.
- Un decompilador sigue mostrando la lógica (con nombres cambiados) y una IA puede ayudar a entenderla.
- El cifrado de cadenas dentro del propio APK solo retrasa: la clave viaja con la app.

La única protección real es que **el secreto no esté en la app**: que la app pida las descargas a un servidor tuyo
que sea el único que conoce la IP, el SNI y las cabeceras. Si el truco depende de que el tráfico salga
del teléfono por esa IP, eso no es posible; en ese caso el objetivo realista es dificultar y desanimar
(lo que hace este proyecto), no impedir.
