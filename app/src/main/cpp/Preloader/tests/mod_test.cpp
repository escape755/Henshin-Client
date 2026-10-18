#include "Module.h"
#include <dlfcn.h>
#include <cstdio>
#include <cstring>
using namespace pl::impl;
int main(){
  Module m; if(!findModule("libc.so.6",m)){puts("libc no encontrada");return 1;}
  printf("libc: %s exec segs=%zu\n",m.path.c_str(),m.exec.size());
  void* h=dlopen("libc.so.6",RTLD_NOW);
  int bad=0,tot=0;
  for(const char* s:{"malloc","strlen","memcpy","printf","dlopen","pthread_create","qsort","getenv","__libc_start_main"}){
    uintptr_t a=lookupSymbol(m,s); void* d=dlsym(h,s); tot++;
    bool eq=a==(uintptr_t)d; if(!eq)bad++; printf("%-20s mine=%lx dlsym=%lx %s\n",s,a,(uintptr_t)d,eq?"OK":"DIFF");
  }
  printf("inexistente -> %lx\n",lookupSymbol(m,"no_existe_xyz"));
  // patrón: 8 bytes de strlen con 2 comodines
  unsigned char* p=(unsigned char*)dlsym(h,"qsort"); char pat[128]; 
  snprintf(pat,sizeof pat,"%02X %02X ? ? %02X %02X %02X %02X",p[0],p[1],p[4],p[5],p[6],p[7]);
  uintptr_t r=scanPattern(m,pat); printf("patron '%s' -> %lx (qsort=%lx)\n",pat,r,(uintptr_t)p);
  printf("looksLikePattern: %d %d %d\n",looksLikePattern("? ? ? 96 ? A9"),looksLikePattern("_ZN9Micro"),looksLikePattern("DEADBEEF"));
  return bad;
}
