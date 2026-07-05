# Minecraft NPO TV

Minecraft NPO TV is een Fabric mod voor Minecraft Java 26.2. De mod voegt een plaatsbare televisie toe waarmee je in-game NPO 1, NPO 2, NPO 3 en een custom kanaal kunt kiezen.

De TV heeft twee manieren om te kijken:

- Zonder stream-config toont het blok een stilstaand NPO-scherm per kanaal.
- Met een directe HLS/media-URL kan de client via VLCJ/libVLC bewegend beeld en geluid afspelen op het TV-scherm.

De mod embedt geen officiele NPO-logo's of NPO-video's. De knop `Open stream` opent de officiele NPO-pagina in je browser.

## Features

- Block/item id: `minecraft_tv:television`
- GUI met knoppen voor `NPO 1`, `NPO 2`, `NPO 3`, `Custom`, `Uit` en `Open stream`
- Kanaalstatus op het block: `off`, `npo1`, `npo2`, `npo3`, `custom`
- Multiplayer-safe channel updates via server packet `minecraft_tv:set_channel`
- Client-side stream playback met VLCJ/libVLC
- Fallback textures als er geen stream is of VLC niet werkt
- Grote TV-muur: aangrenzende TV-blokken met dezelfde kijkrichting vormen samen een groot beeld
- GitHub Actions workflow die automatisch een jar bouwt en uploadt als artifact

## Installatie

1. Installeer Minecraft Java 26.2 met Fabric Loader.
2. Installeer Fabric API voor Minecraft 26.2.
3. Bouw de mod lokaal of download de jar uit GitHub Actions.
4. Zet de jar in je Minecraft mods-map:

```text
~/Library/Application Support/minecraft/mods/
```

## Gebruik In Game

1. Start Minecraft met Fabric.
2. Pak het blok `minecraft_tv:television` uit de creative tab of gebruik:

```text
/give @p minecraft_tv:television
```

3. Plaats de TV.
4. Rechtermuisklik op de TV.
5. Kies `NPO 1`, `NPO 2`, `NPO 3`, `Custom` of `Uit`.
6. Gebruik `Open stream` om de officiele NPO-pagina of de ingestelde custom URL in je browser te openen.

## Grote TV-Muur

Plaats meerdere TV-blokken direct naast elkaar of boven elkaar. Als ze dezelfde kant op kijken, worden ze automatisch een groot scherm.

Voorbeelden:

- `3 x 1`: breed scherm
- `3 x 2`: videowall
- maximaal `16 x 9`

Als je op een TV in de muur een kanaal kiest, schakelt de hele muur mee. Voor live playback gebruikt de muur maar een VLC-sessie, zodat audio niet meerdere keren tegelijk afspeelt.

## Stream Config

Na de eerste start maakt de mod dit config-bestand aan:

```text
config/minecraft_tv_streams.properties
```

Als je dit bestand al had voordat het custom kanaal bestond, voeg `custom=` dan handmatig toe. Bestaande config-bestanden worden niet automatisch herschreven.

Voor echte in-game video moet je daar directe media-URL's invullen, bijvoorbeeld een vrije HLS teststream:

```properties
npo1=
npo2=
npo3=
custom=https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8
```

Belangrijk: `custom=` is bedoeld voor een directe media-URL. Een gewone webpagina zoals `https://npo.nl/start/live/npo3` of een normale YouTube-link werkt hier niet betrouwbaar. De mod heeft een directe `.m3u8`, `.mp4` of vergelijkbare media-URL nodig.

NPO's eigen livestreams kunnen DRM gebruiken. Een DRM-beveiligde NPO `.m3u8` lijkt op een stream-URL, maar VLC kan die meestal niet afspelen zonder de officiele NPO-player/licentieflow. Gebruik dan de knop `Open stream`.

## VLC

Voor bewegende video en geluid in Minecraft heeft de client VLC/libVLC nodig. Op macOS kan dat bijvoorbeeld met Homebrew:

```bash
brew install --cask vlc
```

Als VLC niet beschikbaar is of playback faalt, crasht de mod niet. De TV valt terug naar het statische kanaalscherm.

## Build

Lokaal bouwen:

```bash
./gradlew build
```

De jar komt daarna in:

```text
app/build/libs/
```

GitHub bouwt automatisch bij push naar `main`, bij pull requests en via handmatige workflow-start. Open op GitHub de tab `Actions`, kies de laatste `Build Mod Jar` run en download het artifact `minecraft-tv-jar`. Lokaal bouwen met `./gradlew build` blijft ook werken.

## Troubleshooting

- TV in je hand is paars/zwart: gebruik de nieuwste jar en controleer dat `assets/minecraft_tv/items/television.json` in de jar zit.
- Custom kanaal speelt niet: controleer dat `custom=` een directe `.m3u8`, `.mp4` of andere media-URL bevat.
- NPO- of YouTube-webpagina's werken niet als directe stream; gebruik daarvoor `Open stream` in de browser.
- VLC/libVLC ontbreekt of werkt niet: de mod crasht niet, maar toont het statische fallback scherm.

## Technische Details

- Minecraft: `26.2`
- Fabric Loader: `0.19.2`
- Fabric API: `0.154.0+26.2`
- Fabric Loom: `1.16.2`
- Java: `25`
- Mod id: `minecraft_tv`
- Package: `minecrfat.tv`

## Beperkingen

- De mod bevat geen echte videodecoder van zichzelf; live playback gebruikt VLCJ/libVLC.
- Dedicated servers spelen nooit video of audio af.
- Elke client speelt streams lokaal af.
- Officiele NPO-webpagina's, redirects, DRM, login en geo-restricties worden door NPO bepaald.
