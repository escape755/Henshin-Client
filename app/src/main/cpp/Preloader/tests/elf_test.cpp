#include "Module.h"
#include <elf.h>
#include <sys/mman.h>
#include <cstdio>
#include <cstring>
#include <cstdlib>
#include <vector>
using namespace pl::impl;
int main(int argc,char**argv){
  FILE*f=fopen(argv[1],"rb"); fseek(f,0,SEEK_END); size_t sz=ftell(f); fseek(f,0,SEEK_SET);
  std::vector<uint8_t> file(sz); fread(file.data(),1,sz,f); fclose(f);
  auto* eh=(Elf64_Ehdr*)file.data(); auto* ph=(Elf64_Phdr*)(file.data()+eh->e_phoff);
  size_t top=0; for(int i=0;i<eh->e_phnum;i++) if(ph[i].p_type==PT_LOAD) top=std::max<size_t>(top,ph[i].p_vaddr+ph[i].p_memsz);
  uint8_t* base=(uint8_t*)mmap(nullptr,top,PROT_READ|PROT_WRITE,MAP_PRIVATE|MAP_ANONYMOUS,-1,0);
  Module m; m.bias=(uintptr_t)base;
  for(int i=0;i<eh->e_phnum;i++){ if(ph[i].p_type==PT_LOAD) memcpy(base+ph[i].p_vaddr,file.data()+ph[i].p_offset,ph[i].p_filesz);
     if(ph[i].p_type==PT_DYNAMIC) m.dynamic=m.bias+ph[i].p_vaddr; }
  int bad=0,n=0; char name[1024]; unsigned long addr;
  while(scanf("%lx %1023s",&addr,name)==2){ n++; uintptr_t r=lookupSymbol(m,name); bool ok=(r==m.bias+addr); if(!ok){bad++; if(bad<5)printf("DIFF %s mine=%lx want=%lx\n",name,r-m.bias,addr);} }
  printf("%s: %d simbolos comprobados, %d diferencias\n",argv[1],n,bad);
  printf("inexistente -> %lx\n",lookupSymbol(m,"__no_existe__"));
}
