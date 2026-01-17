package app.leesh.tratic.chart.service;

import java.time.Instant;

import app.leesh.tratic.chart.domain.ChartSignature;

public record ChartFetchRequest(
        ChartSignature sig,
        Instant asOf,
        long count) {
}