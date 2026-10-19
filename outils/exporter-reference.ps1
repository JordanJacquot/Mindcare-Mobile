# Refait la référence du test d'équivalence (app/src/test/resources/reference-pc.json) : des milliers d'enquêtes au
# hasard et de recherches, jouées par le VRAI moteur de Mindcare PC (modules\enquete.ps1) sur la copie du catalogue
# de l'appli. Le test Kotlin (EquivalenceTest) rejoue les mêmes et doit trouver exactement les mêmes résultats.
#
# À relancer quand le catalogue ou la logique change sur PC (le test le signale), puis porter le changement de logique
# en Kotlin jusqu'à ce que le test passe.
#   powershell -ExecutionPolicy Bypass -File outils\exporter-reference.ps1
param(
    [string]$Pc = 'D:\Projets\Mindcare\Mindcare\fichiers',
    [int]$ParPanne = 40,
    [int]$Graine = 20261019
)
$ErrorActionPreference = 'Stop'
$racine = Split-Path $PSScriptRoot -Parent
$AppDir = $Pc
. (Join-Path $Pc 'modules\enquete.ps1')
$copie = Join-Path $racine 'app\src\main\assets\catalogue\enquetes.json'
# (la copie de l'appli doit être celle du PC : sinon, la construction ne l'a pas encore recopiée)
if ([IO.File]::ReadAllText($copie) -ne [IO.File]::ReadAllText((Join-Path $Pc 'enquetes.json'))) { throw "La copie du catalogue n'est pas celle du PC : lance d'abord « gradlew preBuild »." }
$d = Read-Enquetes $copie
$hasard = New-Object System.Random $Graine

function Get-Cas($Panne, [int[]]$Choix) {
    $e = Get-Enquete $d $Panne $Choix
    [ordered]@{
        Panne = [string]$Panne.Id; Entree = @($Choix)
        Etape = $(if ($e.Etape) { [string]$e.Etape.Id } else { '' }); Choix = @($e.Choix | ForEach-Object { [int]$_ })
        Restants = @($e.Restants); Ecartes = @($e.Ecartes | ForEach-Object { "$($_.Id)|$($_.Raison)" })
        Notes = @($e.Notes); Preuves = @($e.Preuves); Faits = @($e.Faits | ForEach-Object { "$($_.Court)|$($_.Reponse)" })
        Verdict = $(if ($e.Verdict) { "$($e.Verdict.Type)|$($e.Verdict.Suspect)|$($e.Verdict.Detail)|$(@($e.Verdict.Restants) -join ',')" } else { '' })
        Titre = (Get-VerdictTitre $d $Panne $e.Verdict); AVenir = [int]$e.AVenir
    }
}

$cas = New-Object System.Collections.Generic.List[object]
foreach ($p in $d.Pannes) {
    $cas.Add((Get-Cas $p @()))
    for ($k = 0; $k -lt $ParPanne; $k++) {
        # Une enquête au hasard jusqu'au bout (une fois sur dix : « Je ne sais pas »), et un arrêt en route
        $choix = @()
        for ($t = 0; $t -lt 60; $t++) {
            $e = Get-Enquete $d $p $choix
            if (-not $e.Etape) { break }
            $choix += $(if ($hasard.Next(10) -eq 0) { -1 } else { $hasard.Next(@($e.Etape.Reponses).Count) })
        }
        $cas.Add((Get-Cas $p $choix))
        if ($choix.Count -gt 1) { $cas.Add((Get-Cas $p @($choix | Select-Object -First ($hasard.Next($choix.Count - 1) + 1)))) }
        if ($k % 10 -eq 0) { $cas.Add((Get-Cas $p (@($choix) + 0 + 5))) }   # (des réponses en trop, ou hors liste)
    }
}

# Les recherches : les noms, phrases et mots du catalogue, des morceaux, et des phrases de tous les jours
$requetes = New-Object System.Collections.Generic.List[string]
foreach ($p in $d.Pannes) {
    $requetes.Add([string]$p.Titre); $requetes.Add([string]$p.Phrase)
    foreach ($m in $p.Mots) { $requetes.Add([string]$m) }
    $mots = @(([string]$p.Titre) -split ' ')
    if ($mots.Count -gt 2) { $requetes.Add(($mots | Select-Object -Skip 1 -First 2) -join ' ') }
}
foreach ($f in $d.Familles) { $requetes.Add([string]$f.Titre) }
foreach ($q in 'il redémarre', 'mon pc fait des bips', 'le wifi se coupe toutes les 5 minutes', 'écran bleu MEMORY_MANAGEMENT', '0x1A',
               "j'ai plus de son", 'le son grésille', 'le pc démarre sur le bios', 'ma webcam marche pas', "l'ordi est super lent",
               'ça chauffe', 'Le PC  ne   démarre PAS !', 'œil', 'ÉCRAN NOIR', 'clé usb', 'a', '', '   ', 'zzzz', 'mémoire insuffisante') { $requetes.Add($q) }
$recherches = foreach ($q in $requetes) { [ordered]@{ Requete = $q; Pannes = @(Find-Pannes $d $q | ForEach-Object { [string]$_.Id }) } }

$sortie = Join-Path $racine 'app\src\test\resources\reference-pc.json'
$json = [ordered]@{ Graine = $Graine; Cas = $cas.ToArray(); Recherches = @($recherches) } | ConvertTo-Json -Depth 6 -Compress
[IO.File]::WriteAllText($sortie, $json, (New-Object System.Text.UTF8Encoding $false))
"Référence : $($cas.Count) enquêtes, $(@($recherches).Count) recherches -> $sortie"
