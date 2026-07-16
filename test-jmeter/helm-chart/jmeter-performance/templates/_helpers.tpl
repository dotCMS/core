{{/*
Expand the name of the chart.
*/}}
{{- define "jmeter-performance.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "jmeter-performance.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "jmeter-performance.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "jmeter-performance.labels" -}}
helm.sh/chart: {{ include "jmeter-performance.chart" . }}
{{ include "jmeter-performance.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- range $key, $value := .Values.labels }}
{{ $key }}: {{ $value | quote }}
{{- end }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "jmeter-performance.selectorLabels" -}}
app.kubernetes.io/name: {{ include "jmeter-performance.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
JMeter startup command
*/}}
{{- define "jmeter-performance.startupCommand" -}}
apk add --no-cache openjdk11-jre curl bash && wget {{ .Values.jmeter.downloadUrl }} && tar -xzf apache-jmeter-{{ .Values.jmeter.version }}.tgz && mv apache-jmeter-{{ .Values.jmeter.version }} /opt/jmeter && export PATH=/opt/jmeter/bin:$PATH && while true; do sleep 3600; done
{{- end }} 