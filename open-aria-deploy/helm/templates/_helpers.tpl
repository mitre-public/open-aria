{{/*
Expand the name of the chart.
*/}}
{{- define "open-aria.name" -}}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "open-aria.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "open-aria.labels" -}}
helm.sh/chart: {{ include "open-aria.chart" . }}
{{ include "open-aria.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "open-aria.selectorLabels" -}}
app.kubernetes.io/name: {{ include "open-aria.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Hashed Config Map
*/}}
{{- define "open-aria.configSha256" -}}
{{ .Values.configuration | toYaml | sha256sum | trunc 8 }}
{{- end }}
{{- define "open-aria.configMapName" -}}
{{- printf "%s-%s" (.Chart.Name)  (include "open-aria.configSha256" .) }}
{{- end }}


{{/*
aria.yaml template
*/}}
{{- define "ariaConfigTemplate" -}}
inputKafkaPropFile: /config/consumer.properties
kafkaPartitionMappingFile: /config/partitions.txt
loggingPeriodSec: {{ .Values.configuration.aria.yaml.loggingPeriodSec }}
logSinkSuppliers:
  - pluginClass: org.mitre.openaria.airborne.config.StdOutSinkSupplier
airborneConfig:
  algorithmDef:
    dataFormat: {{ .Values.configuration.aria.yaml.airborneConfig.algorithmDef.dataFormat }}
    hostId: {{ .Values.configuration.aria.yaml.airborneConfig.algorithmDef.hostId }}
    maxReportableScore: {{ .Values.configuration.aria.yaml.airborneConfig.algorithmDef.maxReportableScore }}
    filterByAirspace: {{ .Values.configuration.aria.yaml.airborneConfig.algorithmDef.filterByAirspace }}
    requiredDiverganceDistInNM: {{ .Values.configuration.aria.yaml.airborneConfig.algorithmDef.requiredDiverganceDistInNM }}
    onGroundSpeedInKnots: {{ .Values.configuration.aria.yaml.airborneConfig.algorithmDef.onGroundSpeedInKnots }}
    requiredTimeOverlapInMs: {{ .Values.configuration.aria.yaml.airborneConfig.algorithmDef.requiredTimeOverlapInMs }}
    formationFilters: {{ .Values.configuration.aria.yaml.airborneConfig.algorithmDef.formationFilters }}
    requiredProximityInNM: {{ .Values.configuration.aria.yaml.airborneConfig.algorithmDef.requiredProximityInNM }}
    sizeOfTrackSmoothingCache: {{ .Values.configuration.aria.yaml.airborneConfig.algorithmDef.sizeOfTrackSmoothingCache }}
    trackSmoothingExpirationSec: {{ .Values.configuration.aria.yaml.airborneConfig.algorithmDef.trackSmoothingExpirationSec }}
    logDuplicateTracks: {{ .Values.configuration.aria.yaml.airborneConfig.algorithmDef.logDuplicateTracks }}
    applySmoothing: {{ .Values.configuration.aria.yaml.airborneConfig.algorithmDef.applySmoothing }}
    requireDataTag: {{ .Values.configuration.aria.yaml.airborneConfig.algorithmDef.requireDataTag }}
    publishAirborneDynamics: {{ .Values.configuration.aria.yaml.airborneConfig.algorithmDef.publishAirborneDynamics }}
    publishTrackData: {{ .Values.configuration.aria.yaml.airborneConfig.algorithmDef.publishTrackData }}
    verbose: {{ .Values.configuration.aria.yaml.airborneConfig.algorithmDef.verbose }}
    logFileDirectory: {{ .Values.configuration.aria.yaml.airborneConfig.algorithmDef.logFileDirectory }}
    airborneDynamicsRadiusNm: {{ .Values.configuration.aria.yaml.airborneConfig.algorithmDef.airborneDynamicsRadiusNm }}
  trackPairingDistanceInNM: {{ .Values.configuration.aria.yaml.airborneConfig.trackPairingDistanceInNM }}
  inMemorySortBufferSec: {{ .Values.configuration.aria.yaml.airborneConfig.inMemorySortBufferSec}}
outputConfig:
  outputSinkSuppliers:
    - pluginClass: org.mitre.openaria.airborne.config.AirborneKafkaSinkSupplier
      configOptions:
        topic: {{ .Values.configuration.aria.yaml.airborneEventsTopic }}
        kafkaPropFile: /config/producer.properties
        kafkaPartitionMappingFile: /config/partitions.txt
dataProcessingOptions:
  pointPrefetchLimit: {{ .Values.configuration.aria.yaml.dataProcessingOptions.pointPrefetchLimit }}
  numWorkerThreads: {{ .Values.configuration.aria.yaml.dataProcessingOptions.numWorkerThreads }}
  milliSecBtwPollAttempts: {{ .Values.configuration.aria.yaml.dataProcessingOptions.milliSecBtwPollAttempts }}
  useConsumerGroups: {{ .Values.configuration.aria.yaml.dataProcessingOptions.useConsumerGroups }}
  minPartition: {{ .Values.configuration.aria.yaml.dataProcessingOptions.minPartition }}
  maxPartition: {{ .Values.configuration.aria.yaml.dataProcessingOptions.maxPartition }}
  pointTopic: {{ .Values.configuration.aria.yaml.pointTopic }}
{{- end -}}