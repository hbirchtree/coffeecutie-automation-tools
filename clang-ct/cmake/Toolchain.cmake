# CMake cache for a single cross-capable Clang/LLD toolchain with static
# LLVM runtimes (compiler-rt builtins + crt, libunwind, libc++abi, libc++)
# for every triple in CLANG_CT_TARGETS.
#
# Modelled on clang/cmake/caches/Fuchsia-stage2.cmake.
#
# Expected -D variables from the Makefile:
#   CLANG_CT_SYSROOT_DIR  directory holding one glibc sysroot per triple

set(CLANG_CT_TARGETS
  x86_64-unknown-linux-gnu
  aarch64-unknown-linux-gnu
  armv7-unknown-linux-gnueabihf
  armv6-unknown-linux-gnueabihf
  CACHE STRING "")

# Per-target compile flags; must agree with clang-ct/config/<triple>.cfg
set(CLANG_CT_FLAGS_x86_64-unknown-linux-gnu       "")
set(CLANG_CT_FLAGS_aarch64-unknown-linux-gnu      "")
set(CLANG_CT_FLAGS_armv7-unknown-linux-gnueabihf "-mfloat-abi=hard -mfpu=neon")
set(CLANG_CT_FLAGS_armv6-unknown-linux-gnueabihf  "-mfloat-abi=hard -mfpu=vfpv2 -marm -mcpu=arm1176jzf-s")

#
# Host tools
#
set(CMAKE_BUILD_TYPE Release CACHE STRING "")
set(LLVM_ENABLE_PROJECTS "clang;lld" CACHE STRING "")
set(LLVM_ENABLE_RUNTIMES "compiler-rt;libunwind;libcxxabi;libcxx" CACHE STRING "")
set(LLVM_TARGETS_TO_BUILD "X86;AArch64;ARM" CACHE STRING "")

set(LLVM_ENABLE_ASSERTIONS OFF CACHE BOOL "")
set(LLVM_ENABLE_PER_TARGET_RUNTIME_DIR ON CACHE BOOL "")
set(LLVM_INSTALL_TOOLCHAIN_ONLY ON CACHE BOOL "")
set(LLVM_INCLUDE_TESTS OFF CACHE BOOL "")
set(LLVM_INCLUDE_EXAMPLES OFF CACHE BOOL "")
set(LLVM_INCLUDE_BENCHMARKS OFF CACHE BOOL "")
set(LLVM_INCLUDE_DOCS OFF CACHE BOOL "")
# Keep the host binaries free of optional shared-library dependencies
set(LLVM_ENABLE_ZLIB OFF CACHE BOOL "")
set(LLVM_ENABLE_ZSTD OFF CACHE BOOL "")
set(LLVM_ENABLE_LIBXML2 OFF CACHE BOOL "")
set(LLVM_ENABLE_LIBEDIT OFF CACHE BOOL "")
set(LLVM_ENABLE_LIBPFM OFF CACHE BOOL "")
set(LLVM_STATIC_LINK_CXX_STDLIB ON CACHE BOOL "")

# Make the resulting clang a pure LLVM toolchain; no GCC install required
set(CLANG_DEFAULT_CXX_STDLIB libc++ CACHE STRING "")
set(CLANG_DEFAULT_RTLIB compiler-rt CACHE STRING "")
set(CLANG_DEFAULT_UNWINDLIB libunwind CACHE STRING "")
set(CLANG_DEFAULT_LINKER lld CACHE STRING "")
set(CLANG_DEFAULT_OBJCOPY llvm-objcopy CACHE STRING "")
set(CLANG_PLUGIN_SUPPORT OFF CACHE BOOL "")

#
# Target runtimes
#
set(LLVM_BUILTIN_TARGETS "${CLANG_CT_TARGETS}" CACHE STRING "")
set(LLVM_RUNTIME_TARGETS "${CLANG_CT_TARGETS}" CACHE STRING "")

foreach(target ${CLANG_CT_TARGETS})
  set(sysroot "${CLANG_CT_SYSROOT_DIR}/${target}")
  set(flags "${CLANG_CT_FLAGS_${target}}")

  # compiler-rt builtins + crtbegin/crtend
  set(BUILTINS_${target}_CMAKE_SYSTEM_NAME Linux CACHE STRING "")
  set(BUILTINS_${target}_CMAKE_BUILD_TYPE Release CACHE STRING "")
  set(BUILTINS_${target}_CMAKE_SYSROOT "${sysroot}" CACHE STRING "")
  set(BUILTINS_${target}_CMAKE_C_FLAGS "${flags}" CACHE STRING "")
  set(BUILTINS_${target}_CMAKE_ASM_FLAGS "${flags}" CACHE STRING "")
  set(BUILTINS_${target}_COMPILER_RT_BUILD_CRT ON CACHE BOOL "")
  # armv6 lacks 64-bit atomics; provide __atomic_* instead of needing libatomic
  set(BUILTINS_${target}_COMPILER_RT_EXCLUDE_ATOMIC_BUILTIN OFF CACHE BOOL "")

  set(RUNTIMES_${target}_CMAKE_SYSTEM_NAME Linux CACHE STRING "")
  set(RUNTIMES_${target}_CMAKE_BUILD_TYPE Release CACHE STRING "")
  set(RUNTIMES_${target}_CMAKE_SYSROOT "${sysroot}" CACHE STRING "")
  set(RUNTIMES_${target}_CMAKE_C_FLAGS "${flags}" CACHE STRING "")
  set(RUNTIMES_${target}_CMAKE_CXX_FLAGS "${flags}" CACHE STRING "")
  set(RUNTIMES_${target}_CMAKE_ASM_FLAGS "${flags}" CACHE STRING "")
  set(RUNTIMES_${target}_LLVM_ENABLE_RUNTIMES "${LLVM_ENABLE_RUNTIMES}" CACHE STRING "")
  # Allow the static runtimes to be linked into shared objects as well
  set(RUNTIMES_${target}_CMAKE_POSITION_INDEPENDENT_CODE ON CACHE BOOL "")

  # compiler-rt in the runtimes pass: builtins come from the pass above,
  # everything else is unneeded
  set(RUNTIMES_${target}_COMPILER_RT_USE_BUILTINS_LIBRARY ON CACHE BOOL "")
  set(RUNTIMES_${target}_COMPILER_RT_BUILD_SANITIZERS OFF CACHE BOOL "")
  set(RUNTIMES_${target}_COMPILER_RT_BUILD_XRAY OFF CACHE BOOL "")
  set(RUNTIMES_${target}_COMPILER_RT_BUILD_LIBFUZZER OFF CACHE BOOL "")
  set(RUNTIMES_${target}_COMPILER_RT_BUILD_PROFILE OFF CACHE BOOL "")
  set(RUNTIMES_${target}_COMPILER_RT_BUILD_MEMPROF OFF CACHE BOOL "")
  set(RUNTIMES_${target}_COMPILER_RT_BUILD_ORC OFF CACHE BOOL "")
  set(RUNTIMES_${target}_COMPILER_RT_BUILD_CTX_PROFILE OFF CACHE BOOL "")
  set(RUNTIMES_${target}_COMPILER_RT_BUILD_GWP_ASAN OFF CACHE BOOL "")

  # libunwind: static only
  set(RUNTIMES_${target}_LIBUNWIND_ENABLE_SHARED OFF CACHE BOOL "")
  set(RUNTIMES_${target}_LIBUNWIND_USE_COMPILER_RT ON CACHE BOOL "")

  # libc++abi: static only, folded into libc++.a
  set(RUNTIMES_${target}_LIBCXXABI_ENABLE_SHARED OFF CACHE BOOL "")
  set(RUNTIMES_${target}_LIBCXXABI_USE_COMPILER_RT ON CACHE BOOL "")
  set(RUNTIMES_${target}_LIBCXXABI_USE_LLVM_UNWINDER ON CACHE BOOL "")
  set(RUNTIMES_${target}_LIBCXXABI_ENABLE_STATIC_UNWINDER ON CACHE BOOL "")

  # libc++: static only, so -stdlib=libc++ always links it statically
  set(RUNTIMES_${target}_LIBCXX_ENABLE_SHARED OFF CACHE BOOL "")
  set(RUNTIMES_${target}_LIBCXX_USE_COMPILER_RT ON CACHE BOOL "")
  set(RUNTIMES_${target}_LIBCXX_CXX_ABI libcxxabi CACHE STRING "")
  set(RUNTIMES_${target}_LIBCXX_ENABLE_STATIC_ABI_LIBRARY ON CACHE BOOL "")
  set(RUNTIMES_${target}_LIBCXX_STATICALLY_LINK_ABI_IN_STATIC_LIBRARY ON CACHE BOOL "")
  set(RUNTIMES_${target}_LIBCXX_INCLUDE_BENCHMARKS OFF CACHE BOOL "")
endforeach()
