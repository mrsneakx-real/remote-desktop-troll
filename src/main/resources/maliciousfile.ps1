param(
    [string]$Title = "Malicious file detected",
    [string]$Message = "Microsoft Defender Antivirus has detected a dangerous file and quarantined it.",
    [string]$ImagePath = "defender.png"      # default: relative, same dir as script
)

# Allow the script to work regardless of launch location
$ScriptDir = if ($PSScriptRoot)
{
    $PSScriptRoot
}
else
{
    Split-Path -Parent $MyInvocation.MyCommand.Path
}

# Full resource paths
$ImageFullPath = Join-Path $ScriptDir $ImagePath
$SoundPath = Join-Path $ScriptDir 'winerror.wav'

# Initialize Windows toast API
[Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] > $null

# Create toast template (1 image + 2 text)
$template = [Windows.UI.Notifications.ToastNotificationManager]::GetTemplateContent(
[Windows.UI.Notifications.ToastTemplateType]::ToastImageAndText02
)

# Set text
$textNodes = $template.GetElementsByTagName("text")
$textNodes.Item(0).AppendChild($template.CreateTextNode($Title)) | Out-Null
$textNodes.Item(1).AppendChild($template.CreateTextNode($Message)) | Out-Null

# Set image
$imageNodes = $template.GetElementsByTagName("image")
$imageNodes.Item(0).SetAttribute("src", $ImageFullPath)
$imageNodes.Item(0).SetAttribute("alt", "Toast Image")

# Play sound if file exists
if (Test-Path $SoundPath) {
$player = New-Object System.Media.SoundPlayer
$player.SoundLocation = $SoundPath
$player.Load()
}

# Create and show toast
$toast = [Windows.UI.Notifications.ToastNotification]::new($template)
$notifier = [Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier("Windows Security")
$notifier.Show($toast)

if (Test-Path $SoundPath) {
$player.PlaySync()
}