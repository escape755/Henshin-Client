#include "pl/Signature.h"
#include "Module.h"

namespace pl::signature {

uintptr_t pl_resolve_signature(const char *identifier, const char *moduleName) {
    impl::Module m;
    if (!identifier || !impl::findModule(moduleName, m)) return 0;
    if (impl::looksLikePattern(identifier)) return impl::scanPattern(m, identifier);
    return impl::lookupSymbol(m, identifier);
}

} // namespace pl::signature
