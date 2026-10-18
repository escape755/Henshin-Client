#include "A64Relocator.h"
#include <cstdio>
using namespace pl::impl::a64;
int main(){
  struct T{const char*n;uint32_t i;} ts[]={
   {"adrp x8,+1page",0xB0000008},{"adr x1,+0x10",0x10000081},{"ldr x0,lit+0x100",0x58000800},
   {"ldr s0,lit",0x1C000040},{"ldrsw x2,lit",0x98000082},{"prfm lit",0xD8000040},
   {"bl +0x40",0x94000010},{"b -8",0x17FFFFFE},{"b.eq +0x20",0x54000100},{"cbz x3,+8",0xB4000043},
   {"cbnz w4,-4",0x35FFFFE4},{"tbz w5,#3,+0x10",0x36180085},{"tbnz x6,#40,+12",0xB7400066},
   {"stp x29,x30,[sp,#-16]!",0xA9BF7BFD},{"b.al +8",0x5400004E}};
  uint64_t pc=0x7000001000ull;
  for(auto&t:ts){uint32_t src[4]={t.i,0xD503201F,0xD503201F,0xD503201F};uint32_t out[64]={};
    size_t n=buildTrampoline(src,pc,out);
    printf("%s %zu",t.n,n); for(size_t k=0;k<n/4;k++)printf(" %08x",out[k]); printf("\n");}
}
