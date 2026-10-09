"""Check translated UI keys and formatting arguments against actual Java callers."""
import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
folder = root / 'src/main/resources/assets/forgeautofish/lang'
def read(lang):
    values = {}
    for line in (folder / (lang + '.lang')).read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or '=' not in line:
            continue
        key, value = line.split('=', 1)
        if key in values:
            raise RuntimeError('Duplicate translation key: ' + key)
        values[key] = value
    return values
english, chinese = read('en_us'), read('zh_cn')
if set(english) != set(chinese):
    raise RuntimeError('English and Chinese key sets differ.')
for key in english:
    if re.findall(r'%(?:[0-9]+\$)?s', english[key]) != re.findall(r'%(?:[0-9]+\$)?s', chinese[key]):
        raise RuntimeError('Format arguments differ: ' + key)
    if not english[key] or not chinese[key]:
        raise RuntimeError('Empty translation: ' + key)
used = set()
for file in (root / 'src/main/java').rglob('*.java'):
    used.update(re.findall(r'"((?:gui|message|key)\.forgeautofish\.[A-Za-z.]+)"', file.read_text(encoding='utf-8')))
for key in used:
    if key not in english:
        raise RuntimeError('Missing translation for source key: ' + key)
for name in ('recastDelayMs','castRetryMs','startupDelayMs','hookTimeoutMs','dryHookTimeoutMs','ghostTimeoutMs','motionThreshold','soundRadius'):
    if 'gui.forgeautofish.' + name not in english:
        raise RuntimeError('Missing field label: ' + name)
print('PASS: ' + str(len(english)) + ' bilingual keys, source coverage and matching format arguments')
