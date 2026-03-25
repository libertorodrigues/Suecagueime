# Sueca para Android

Aplicação Android simples em Jetpack Compose para jogar Sueca contra 3 bots.

## Funcionalidades

- Ecrã inicial com a opção **Jogar com bots**.
- 4 jogadores: utilizador, parceiro bot e 2 bots adversários.
- Cada jogo distribui 10 cartas por jogador.
- O dealer começa no utilizador e roda no sentido horário a cada novo jogo.
- O trunfo é definido por uma carta aleatória da mão do dealer no início de cada jogo.
- As cartas do utilizador aparecem agrupadas por naipe e ordenadas em ordem crescente.
- Os bots jogam automaticamente, respeitando a obrigação de assistir ao naipe e tentando otimizar a jogada.
- No fim de 10 vazas são contados os pontos:
  - vitória normal (até 90 pontos): +1 jogo
  - vitória com 91+ pontos: +2 jogos
  - capote (120 pontos): +4 jogos
- A primeira equipa a vencer 4 jogos ganha a partida.

## Como correr

```bash
gradle test
```
