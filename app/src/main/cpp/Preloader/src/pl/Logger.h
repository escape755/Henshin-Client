#pragma once
// Logger mínimo sobre logcat. API: pl::log::Logger logger("tag"); logger.error("...");
#include <android/log.h>
#include <cstdarg>
#include <cstdio>
#include <string>
#include <utility>

namespace pl::log {

class Logger {
public:
    explicit Logger(std::string tag) : tag_(std::move(tag)) {}

    void debug(const char *fmt, ...) const __attribute__((format(printf, 2, 3))) {
        va_list ap; va_start(ap, fmt); v(ANDROID_LOG_DEBUG, fmt, ap); va_end(ap);
    }
    void info(const char *fmt, ...) const __attribute__((format(printf, 2, 3))) {
        va_list ap; va_start(ap, fmt); v(ANDROID_LOG_INFO, fmt, ap); va_end(ap);
    }
    void warn(const char *fmt, ...) const __attribute__((format(printf, 2, 3))) {
        va_list ap; va_start(ap, fmt); v(ANDROID_LOG_WARN, fmt, ap); va_end(ap);
    }
    void error(const char *fmt, ...) const __attribute__((format(printf, 2, 3))) {
        va_list ap; va_start(ap, fmt); v(ANDROID_LOG_ERROR, fmt, ap); va_end(ap);
    }

    void debug(const std::string &s) const { __android_log_write(ANDROID_LOG_DEBUG, tag_.c_str(), s.c_str()); }
    void info(const std::string &s)  const { __android_log_write(ANDROID_LOG_INFO,  tag_.c_str(), s.c_str()); }
    void warn(const std::string &s)  const { __android_log_write(ANDROID_LOG_WARN,  tag_.c_str(), s.c_str()); }
    void error(const std::string &s) const { __android_log_write(ANDROID_LOG_ERROR, tag_.c_str(), s.c_str()); }

private:
    void v(int prio, const char *fmt, va_list ap) const {
        char buf[1024];
        vsnprintf(buf, sizeof(buf), fmt, ap);
        __android_log_write(prio, tag_.c_str(), buf);
    }
    std::string tag_;
};

} // namespace pl::log
