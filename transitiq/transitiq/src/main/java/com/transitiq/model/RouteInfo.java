package com.transitiq.model;

import java.util.List;

public record RouteInfo(int id, String busCode, String name, List<Integer> stopIds) {}
