#!/bin/sh
# The superflat self-test (FlatSelfTest) in a fresh superflat world: points eclipse/server.properties at
# level-type FLAT with a preset, on a world of its own (eclipse/flatworld, made new each run), runs the flat group,
# then puts level-type, generator-settings and level-name back (the rest of the suite runs in a Default world).
#   sh tools/flat/selftest.sh [default|grove|grove-decoration|desert-village]
# default:          3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;village             (1.12's Classic Flat)
# grove:            the cherry grove (biome id $grove) with villages, no decoration: none of this mod's blocks
# grove-decoration: the cherry grove with decoration: cherry trees and pink petals
# desert-village:   desert (biome 2) with villages: the desert village's camel
# The grove's biome id is Forge's, given at startup: with this mod and Backport Core alone it is 40. The test logs
# the id it finds, and this script fails a grove run whose world is not the grove.
set -e
root="$(cd "$(dirname "$0")/../.." && pwd)"
props="$root/eclipse/server.properties"
layers='minecraft:bedrock,2*minecraft:dirt,minecraft:grass'
grove=40
want_grove=
case "${1:-default}" in
    default) preset="3;$layers;1;village" ;;
    grove) preset="3;$layers;$grove;village"; want_grove=1 ;;
    grove-decoration) preset="3;$layers;$grove;village,decoration"; want_grove=1 ;;
    desert-village) preset="3;$layers;2;village" ;;
    *) echo "unknown preset $1" >&2; exit 2 ;;
esac
old_level="$(sed -n 's/^level-name=//p' "$props")"
# A literal path: the flat world is always eclipse/flatworld.
(cd "$root/eclipse" && rm -rf flatworld)
sed -i "s/^level-type=.*/level-type=FLAT/; s|^generator-settings=.*|generator-settings=$preset|; s/^level-name=.*/level-name=flatworld/" "$props"
status=0
(cd "$root" && ./gradlew runServer -Pselftest=flat -PdeployDir= -PcoreDeployDir=) || status=$?
sed -i "s/^level-type=.*/level-type=DEFAULT/; s|^generator-settings=.*|generator-settings=|; s/^level-name=.*/level-name=${old_level:-world}/" "$props"
log="$root/eclipse/logs/latest.log"
if [ -n "$want_grove" ] && ! grep -q "SELFTEST flat biome trailsandtalesbackport:cherry_grove" "$log" 2>/dev/null; then
    echo "biome $grove is not the cherry grove in this run:" >&2
    grep -h "SELFTEST flat biome" "$log" >&2 || true
    status=1
fi
grep -h "SELFTEST DONE" "$log" 2>/dev/null | tail -1 || true
grep -q "SELFTEST DONE pass=[0-9]* fail=0" "$log" 2>/dev/null || status=1
exit $status
