# Identidade da linha Arch

O que é **comum** a todos os sistemas da linha — e o que **não** é.

| Compartilhado (este diretório)                   | De cada produto                         |
|--------------------------------------------------|-----------------------------------------|
| Marca da linha (`arch-mark.svg`)                 | Logo e nome do produto                  |
| Gradiente violeta→azul (`--arch-gradient`)       | Paleta, tema claro/escuro, tipografia   |
| Cores de status `UP`/`DOWN`/`UNKNOWN`            | Layout, componentes, tom de voz         |
| Cores de nota de arquitetura (A–F)               |                                         |
| Cor por papel (`role:platform`, `business`, …)   |                                         |
| Assinatura "Parte da linha Arch" (`.arch-signature`) |                                     |

A ideia é a de uma linha de produtos (como uma suíte): você reconhece que
os sistemas são da mesma família pela marca e pelos sinais compartilhados
(status, notas), mas cada um mantém a cara que faz sentido para o seu usuário
— o PDV continua sendo um PDV, o board continua sendo um board.

## Arquivos

- `arch-mark.svg` — marca da linha (arco + ponto: os sistemas conectados ao Hub).
- `arch-line.css` — tokens CSS (`--arch-*`) e a classe `.arch-signature`.
- `arch-line.json` — os mesmos tokens para ferramentas (gerado a partir do CSS).

## Como um produto usa

Os repositórios são independentes, então cada produto **copia** (vendoriza)
`arch-mark.svg` e os tokens que usa, com um comentário apontando para cá.
Hoje:

| Produto     | Onde                                                                 |
|-------------|----------------------------------------------------------------------|
| Arch Hub    | dashboard inteiro (`src/main/resources/static`)                      |
| SIMPLE-ArCh | painel "Linha Arch" + assinatura (`web/public/arch-mark.svg`)        |
| System-PVD  | assinatura no rodapé da sidebar (`frontend/public/arch-mark.svg`)    |
| ArchMAP     | marca no rodapé da barra lateral da web UI (`src/archmap/web-ui/static/arch-mark.svg`), tooltip nos 6 idiomas |

Mudou um token? Edite `arch-line.css`, rode `python3 brand/build-tokens.py`
para regenerar o JSON e copie para os produtos. No Hub, um teste
(`BrandAssetsTest`) falha se a cópia em `static/` divergir desta.
