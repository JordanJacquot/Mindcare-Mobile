# Refait la référence du test d'équivalence (app/src/test/resources/reference-pc.json) : des milliers d'enquêtes au
# hasard et de recherches, les groupes de chaque famille et le tri des pannes selon le PC (« PourPc »), joués par le
# VRAI moteur de Mindcare PC (le moteur Rust : rust\mindcare-moteur, son exemple exporter_reference) sur la copie du
# catalogue de l'appli. Le test Kotlin (EquivalenceTest) rejoue tout et doit trouver exactement les mêmes résultats.
#
# À relancer quand le catalogue ou la logique change sur PC (le test le signale), puis porter le changement de logique
# en Kotlin jusqu'à ce que le test passe. Depuis une console PowerShell :
#   & 'outils\exporter-reference.ps1'
# (-Pc : le dossier Mindcare\fichiers d'une autre copie du dépôt Mindcare ; le moteur est pris dans la même copie)
param(
    [string]$Pc = 'D:\Projets\Mindcare\Mindcare\fichiers',
    [int]$ParPanne = 40,
    [int]$Graine = 20261019
)
$ErrorActionPreference = 'Stop'
$racine = Split-Path $PSScriptRoot -Parent
$rust = Join-Path (Split-Path (Split-Path $Pc -Parent) -Parent) 'rust'
if (-not (Test-Path -LiteralPath (Join-Path $rust 'mindcare-moteur\examples\exporter_reference.rs'))) { throw "Moteur Rust introuvable : $rust" }
$copie = Join-Path $racine 'app\src\main\assets\catalogue\enquetes.json'
# (la copie de l'appli doit être celle du PC : sinon, la construction ne l'a pas encore recopiée)
if ([IO.File]::ReadAllText($copie).Replace("`r`n", "`n") -ne [IO.File]::ReadAllText((Join-Path $Pc 'enquetes.json')).Replace("`r`n", "`n")) { throw "La copie du catalogue n'est pas celle du PC : lance d'abord « gradlew preBuild »." }
$sortie = Join-Path $racine 'app\src\test\resources\reference-pc.json'

# Rust est installé dans D:\Outils\Rust (sans téléchargement : --offline --locked)
if (-not $env:CARGO_HOME) { $env:CARGO_HOME = 'D:\Outils\Rust\cargo' }
if (-not $env:RUSTUP_HOME) { $env:RUSTUP_HOME = 'D:\Outils\Rust\rustup' }
$cargo = Join-Path $env:CARGO_HOME 'bin\cargo.exe'
if (-not (Test-Path -LiteralPath $cargo)) { $cargo = 'cargo' }

# cargo sans nouvelle fenêtre ; ses messages dans des fichiers, montrés seulement s'il échoue
$journal = Join-Path ([IO.Path]::GetTempPath()) "mindcare-reference-$PID"
$arguments = @('run', '--offline', '--locked', '--release', '-q', '-p', 'mindcare-moteur', '--example', 'exporter_reference', '--',
    '--catalogue', "`"$copie`"", '--sortie', "`"$sortie`"", '--par-panne', $ParPanne, '--graine', $Graine)
$p = Start-Process -FilePath $cargo -ArgumentList $arguments -WorkingDirectory $rust -NoNewWindow -Wait -PassThru `
    -RedirectStandardOutput "$journal.txt" -RedirectStandardError "$journal.err"
$texte = (Get-Content -LiteralPath "$journal.txt", "$journal.err" -Encoding UTF8 -ErrorAction SilentlyContinue) -join "`n"
Remove-Item -LiteralPath "$journal.txt", "$journal.err" -ErrorAction SilentlyContinue
if ($p.ExitCode -ne 0) { throw "L'export Rust a échoué (code $($p.ExitCode)) :`n$texte" }
$texte
