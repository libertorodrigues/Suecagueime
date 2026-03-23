# Sueca para Android

Aplicação Android simples em Jetpack Compose para jogar Sueca contra 3 bots.

## Funcionalidades

- Ecrã inicial com a opção **Jogar com bots**.
- 4 jogadores: utilizador, parceiro bot e 2 bots adversários.
- Cada jogo distribui 10 cartas por jogador.
- O trunfo começa no utilizador e roda no sentido horário no início de cada novo jogo.
- As cartas do utilizador aparecem agrupadas por naipe e ordenadas em ordem crescente.
- Os bots jogam automaticamente, respeitando a obrigação de assistir ao naipe quando possível.
- No fim de 10 vazas são contados os pontos e a equipa vencedora do jogo recebe 1 ponto de partida.
- A primeira equipa a vencer 4 jogos ganha a partida.

## Como correr

```bash
gradle test
```
