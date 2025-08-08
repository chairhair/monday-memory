Get-Content .env | ForEach-Object {
  $name, $value = $_ -split '='
  [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
}