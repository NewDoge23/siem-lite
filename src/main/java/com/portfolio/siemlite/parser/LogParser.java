package com.portfolio.siemlite.parser;

import com.portfolio.siemlite.model.LogEvent;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface LogParser {

    List<LogEvent> parse(Path path) throws IOException;
}
