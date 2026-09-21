// Exports reference data from the web game so the Kotlin port can be checked against it.
// Usage (repo root, after `npm install`): node tools/android/export_golden.mjs
import { mkdirSync, writeFileSync } from 'node:fs'
import { build } from 'esbuild'

const bundled = await build({
  stdin: {
    contents: "export * from './src/game/cats.ts'; export * from './src/game/rooms.ts'; export * from './src/game/upgrades.ts'; export * from './src/game/items.ts'; export * from './src/game/balance.ts';",
    resolveDir: process.cwd(),
    sourcefile: 'golden.ts',
  },
  bundle: true, platform: 'node', format: 'esm', write: false,
})
const game = await import(`data:text/javascript;base64,${Buffer.from(bundled.outputFiles[0].contents).toString('base64')}`)

const outDir = 'android/game/src/test/resources/golden'
mkdirSync(outDir, { recursive: true })
const data = {
  rooms: game.rooms,
  cats: game.cats,
  upgrades: game.upgrades,
  resources: game.resources,
  foods: game.foods,
  balance: { expertCostScale: game.expertCostScale, roomScales: [1, 2, 3, 4, 5].map(game.roomEconomyScale) },
}
writeFileSync(`${outDir}/data.json`, JSON.stringify(data, null, 1))
console.log(`data.json: ${game.rooms.length} rooms, ${game.cats.length} cats, ${game.upgrades.length} upgrades`)
