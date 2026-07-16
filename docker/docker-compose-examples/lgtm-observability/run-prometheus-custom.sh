#!/bin/bash

source ./logging.sh

# Use our custom Prometheus configuration that includes dotCMS scraping
run_with_logging "Prometheus ${PROMETHEUS_VERSION}" "${ENABLE_LOGS_PROMETHEUS:-false}" ./prometheus/prometheus \
	--web.enable-remote-write-receiver \
	--web.enable-otlp-receiver \
	--enable-feature=exemplar-storage \
	--enable-feature=native-histograms \
	--storage.tsdb.path=/data/prometheus \
	--config.file=./prometheus/prometheus.yml