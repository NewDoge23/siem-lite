package com.portfolio.siemlite.windows;

@FunctionalInterface
public interface WindowsEventLogCommandRunner {

    WindowsEventLogCommandResult run(WindowsEventLogQuery query);
}
