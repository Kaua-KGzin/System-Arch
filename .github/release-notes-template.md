## Arch Hub {VERSION}

Pacotes nativos para Windows, macOS e Linux, com JRE embutida — não precisa
instalar Java separadamente para rodar.

- **Windows**: baixe `ArchHub-{VERSION}.exe` e execute o instalador.
- **macOS**: baixe `ArchHub-{VERSION}.dmg`, abra e arraste para Applications.
- **Linux**: baixe o `.deb` (`sudo apt install ./ArchHub-{VERSION}.deb`) ou o
  `.tar.gz` portátil (extraia e rode `ArchHub/bin/ArchHub`).

Cada pacote vem com um `.sha256` ao lado, para conferência.

Depois de abrir, o dashboard fica em `http://localhost:8080`. Para acessar
pelo celular, use o botão **"📱 Ver no celular"** no próprio dashboard —
funciona enquanto o celular estiver na mesma rede Wi-Fi/local.

## O que mudou

{CHANGES}

## Antes de baixar: os pacotes não são assinados

Nenhum dos instaladores acima possui assinatura digital (code signing).
Isso pode disparar avisos do sistema operacional na primeira execução:

- **Windows SmartScreen**: clique em "Mais informações" → "Executar assim mesmo".
- **macOS Gatekeeper**: clique com o botão direito no app → "Abrir" (uma vez).

Isso não se resolve no código: exige um certificado de assinatura (pago) por
plataforma. Verifique o `.sha256` antes de executar se estiver em dúvida.
