/*
    Copyright (C) 2012 Klarälvdalens Datakonsult AB, a KDAB Group company, info@kdab.com
    Author: Volker Krause <volker.krause@kdab.com>

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

#include <EGL/egl.h>
//#include <EGL/eglext.h>

#include <EGL/eglplatform.h>
#include <GLES2/gl2.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>

static bool createX11Window(int width, int height, bool hildon = false);
static bool recreateX11Window(int width, int height, EGLint visualId, bool hildon = false);
static void destroyX11Window();

static unsigned long x11Window_ = 0;

struct enum_t {
    EGLint value;
    const char* displayName;
};

static enum_t boolMap[] {
    { EGL_TRUE, "true" },
    { EGL_FALSE, "false" }
};

static enum_t bufferTypeMap[] {
    { EGL_RGB_BUFFER, "RGB" },
    { EGL_LUMINANCE_BUFFER, "Luminance" }
};

static enum_t caveatMap[] {
    { EGL_NONE, "none" },
    { EGL_SLOW_CONFIG, "slow" },
    { EGL_NON_CONFORMANT_CONFIG, "non-conformant" }
};

static enum_t transparentTypeMap[] {
    { EGL_NONE, "none" },
    { EGL_TRANSPARENT_RGB, "transparent RGB" }
};

static enum_t surfaceTypeMap[] {
    { EGL_PBUFFER_BIT, "pbuffer" },
    { EGL_PIXMAP_BIT, "pixmap" },
    { EGL_WINDOW_BIT, "window" },
    { EGL_VG_COLORSPACE_LINEAR_BIT, "VG (linear colorspace)" },
    { EGL_VG_ALPHA_FORMAT_PRE_BIT, "VG (alpha format pre)" },
    { EGL_MULTISAMPLE_RESOLVE_BOX_BIT, "multisample resolve box" },
    { EGL_SWAP_BEHAVIOR_PRESERVED_BIT, "swap behavior preserved" },
#ifdef EGL_STREAM_BIT_KHR
    { EGL_STREAM_BIT_KHR, "stream" },
#endif
};

static enum_t renderableTypeMap[] {
    { EGL_OPENGL_ES_BIT, "OpenGL ES" },
    { EGL_OPENVG_BIT, "OpenVG" },
    { EGL_OPENGL_ES2_BIT, "OpenGL ES2" },
    { EGL_OPENGL_BIT, "OpenGL" },
#ifdef EGL_OPENGL_ES3_BIT
    { EGL_OPENGL_ES3_BIT, "OpenGL ES3" }
#endif
};


struct attrib_t {
    EGLint attribute;
    const char* displayName;
    enum_t* enumMap;
    int enumMapSize;
    bool isFlag;
};

#define A_NUM(x) { x, #x, 0, 0, false }
#define A_MAP(x, map) { x, #x, map, sizeof(map) / sizeof(enum_t), false }
#define A_FLAG(x, map) { x, #x, map, sizeof(map) / sizeof(enum_t), true }

static attrib_t attributes[] {
    A_NUM(EGL_ALPHA_SIZE),
    A_NUM(EGL_ALPHA_MASK_SIZE),
    A_MAP(EGL_BIND_TO_TEXTURE_RGB, boolMap),
    A_MAP(EGL_BIND_TO_TEXTURE_RGBA, boolMap),
    A_NUM(EGL_BLUE_SIZE),
    A_NUM(EGL_BUFFER_SIZE),
    A_MAP(EGL_COLOR_BUFFER_TYPE, bufferTypeMap),
    A_MAP(EGL_CONFIG_CAVEAT, caveatMap),
    A_NUM(EGL_CONFIG_ID),
    A_FLAG(EGL_CONFORMANT, renderableTypeMap),
    A_NUM(EGL_DEPTH_SIZE),
    A_NUM(EGL_GREEN_SIZE),
    A_NUM(EGL_LEVEL),
    A_NUM(EGL_LUMINANCE_SIZE),
    A_NUM(EGL_MAX_PBUFFER_WIDTH),
    A_NUM(EGL_MAX_PBUFFER_HEIGHT),
    A_NUM(EGL_MAX_PBUFFER_PIXELS),
    A_NUM(EGL_MAX_SWAP_INTERVAL),
    A_NUM(EGL_MIN_SWAP_INTERVAL),
    A_MAP(EGL_NATIVE_RENDERABLE, boolMap),
    A_NUM(EGL_NATIVE_VISUAL_ID),
    A_NUM(EGL_NATIVE_VISUAL_TYPE),
    A_NUM(EGL_RED_SIZE),
    A_FLAG(EGL_RENDERABLE_TYPE, renderableTypeMap),
    A_NUM(EGL_SAMPLE_BUFFERS),
    A_NUM(EGL_SAMPLES),
    A_NUM(EGL_STENCIL_SIZE),
    A_FLAG(EGL_SURFACE_TYPE, surfaceTypeMap),
    A_MAP(EGL_TRANSPARENT_TYPE, transparentTypeMap),
    A_NUM(EGL_TRANSPARENT_RED_VALUE),
    A_NUM(EGL_TRANSPARENT_GREEN_VALUE),
    A_NUM(EGL_TRANSPARENT_BLUE_VALUE)
};

#undef A_NUM
#undef A_MAP
#undef A_FLAG

static const int attributesSize = sizeof(attributes) / sizeof(attrib_t);

struct device_property_t {
    EGLint name;
    const char* displayName;
    const char* extension;
    enum Type {
        String,
        Attribute
    } type;
};

static const device_property_t deviceProperties[] {
#ifdef EGL_DRM_DEVICE_FILE_EXT
    { EGL_DRM_DEVICE_FILE_EXT, "DRM device file", "EGL_EXT_device_drm", device_property_t::String },
#endif
#ifdef EGL_CUDA_DEVICE_NV
    { EGL_CUDA_DEVICE_NV, "CUDA device", "EGL_NV_device_cuda", device_property_t::Attribute }
#endif
};

static const int devicePropertiesSize = sizeof(deviceProperties) / sizeof(device_property_t);


static void printEnum(int value, attrib_t *attr)
{
    for (int i = 0; i < attr->enumMapSize; ++i) {
        enum_t *enumValue = &attr->enumMap[i];
        if (value == enumValue->value) {
            printf("%s", enumValue->displayName);
            return;
        }
    }
    printf("0x%xu", value);
}

static void printFlags(int value, attrib_t *attr)
{
    bool firstEntry = true;
    int handledFlags = 0;
    for (int i = 0; i < attr->enumMapSize; ++i) {
        enum_t *enumValue = &attr->enumMap[i];
        if (value & enumValue->value) {
            if (!firstEntry)
                printf(", ");
            printf("%s", enumValue->displayName);
            firstEntry = false;
            handledFlags |= enumValue->value;
        }
    }

    if (handledFlags != value) {
        if (!firstEntry)
            printf(", ");
        printf("unhandled flags 0x%xu", (value - handledFlags));
    }
}

static void printOutputLayers(EGLDisplay display, const char* indent = "")
{
#ifdef EGL_EXT_output_base
    const auto eglGetOutputLayersEXT = reinterpret_cast<PFNEGLGETOUTPUTLAYERSEXTPROC>(eglGetProcAddress("eglGetOutputLayersEXT"));
    if (!eglGetOutputLayersEXT) {
        printf("%sFailed to resolve eglGetOutputLayersEXT function.", indent);
	return;
    }

    EGLint num_layers = 0;
    if (!eglGetOutputLayersEXT(display, nullptr, nullptr, 0, &num_layers)) {
        printf("%sFailed to query output layers.", indent);
        return;
    }
    printf("%sFound %i output layers.", indent, num_layers);
#endif
}

static void printOutputPorts(EGLDisplay display, const char* indent = "")
{
#ifdef EGL_EXT_output_base
    const auto eglGetOutputPortsEXT = reinterpret_cast<PFNEGLGETOUTPUTPORTSEXTPROC>(eglGetProcAddress("eglGetOutputPortsEXT"));
    if (!eglGetOutputPortsEXT) {
        printf("%sFailed to resolve eglGetOutputPortsEXT function.\n");
	return;
    }

    EGLint num_ports = 0;
    if (!eglGetOutputPortsEXT(display, nullptr, nullptr, 0, &num_ports)) {
        printf("%sFailed to query output ports.\n", indent);
        return;
    }
    printf("%sFound %i output ports.", indent, num_ports);
#endif
}

static void printDisplay(EGLDisplay display, const char* indent = "")
{
    EGLint majorVersion, minorVersion;
    if (!eglInitialize(display, &majorVersion, &minorVersion)) {
        fprintf(stderr, "Could not initialize EGL!\n");
        exit(1);
    }

    printf("%sEGL version: %i.%i\n", indent, majorVersion, minorVersion);
    const char* clientAPIs = eglQueryString(display, EGL_CLIENT_APIS);
    printf("%sClient APIs for display: %s\n", indent, clientAPIs);
    const char* vendor = eglQueryString(display, EGL_VENDOR);
    printf("%sVendor: %s\n", indent, vendor);
    const char* displayExts = eglQueryString(display, EGL_EXTENSIONS);
    printf("%sDisplay extensions: %s\n", indent, displayExts);

    if (displayExts && strstr(displayExts, "EGL_EXT_output_base") != nullptr) {
        printOutputLayers(display, indent);
        printOutputPorts(display, indent);
    }

    EGLint numConfigs;
    if (!eglGetConfigs(display, 0, 0, &numConfigs) && numConfigs > 0) {
        fprintf(stderr, "Could not retrieve the number of EGL configurations!\n");
        exit(1);
    }

    printf("%sFound %i configurations.\n", indent, numConfigs);

    EGLConfig *configs = reinterpret_cast<EGLConfig*>(malloc(numConfigs * sizeof(EGLint)));
    if (!eglGetConfigs(display, configs, numConfigs, &numConfigs)) {
        fprintf(stderr, "Could not retrieve EGL configurations!\n");
        exit(1);
    }

    for (int i = 0; i < numConfigs; ++i) {
        printf("%sConfiguration %i:\n", indent, i);
        for (int j = 0; j < attributesSize; ++j) {
            attrib_t *attr = &attributes[j];
            EGLint value;
            EGLBoolean result = eglGetConfigAttrib(display, configs[i], attr->attribute, &value);
            printf("%s  %s: ", indent, attr->displayName);
            if (result) {
                if (attr->enumMap) {
                    if (!attr->isFlag)
                        printEnum(value, attr);
                    else
                        printFlags(value, attr);
                } else {
                    printf("%i", value);
                }
            } else {
                printf("<failed>");
            }
            printf("\n");
        }
        printf("\n");
    }

    EGLConfig selectedConfig = nullptr;
    for (int i = 0; i < numConfigs; ++i) {
        EGLint surfaceType = 0, renderableType = 0;
        eglGetConfigAttrib(display, configs[i], EGL_SURFACE_TYPE, &surfaceType);
        eglGetConfigAttrib(display, configs[i], EGL_RENDERABLE_TYPE, &renderableType);
        if ((surfaceType & EGL_WINDOW_BIT) && (renderableType & EGL_OPENGL_ES2_BIT)) {
            selectedConfig = configs[i];
            printf("Using config %i for context creation.\n", i);
            break;
        }
    }

    free(configs);

    if (!selectedConfig) {
        printf("No config found supporting EGL_WINDOW_BIT + EGL_OPENGL_ES2_BIT.\n");
        return;
    }

    EGLint nativeVisualId = 0;
    eglGetConfigAttrib(display, selectedConfig, EGL_NATIVE_VISUAL_ID, &nativeVisualId);
    if (nativeVisualId && !recreateX11Window(128, 128, nativeVisualId, true)) {
        printf("Failed to recreate X11 window with EGL native visual.\n");
        return;
    }

    eglBindAPI(EGL_OPENGL_ES_API);

    EGLint context_params[] = {
        EGL_CONTEXT_CLIENT_VERSION, 2,
        EGL_NONE,
    };

    EGLSurface surface = eglCreateWindowSurface(display, selectedConfig, (EGLNativeWindowType)x11Window_, nullptr);
    if (surface == EGL_NO_SURFACE) {
        printf("No surface created (EGL error: 0x%x)\n", eglGetError());
        return;
    }
    auto context = eglCreateContext(display, selectedConfig, EGL_NO_CONTEXT, context_params);
    if (context == EGL_NO_CONTEXT) {
        printf("No context created (EGL error: 0x%x)\n", eglGetError());
        eglDestroySurface(display, surface);
        return;
    }
    if (eglMakeCurrent(display, surface, surface, context) == EGL_FALSE) {
        printf("Failed to make context current (EGL error: 0x%x)\n", eglGetError());
        return;
    }

    printf("OpenGL ES info:\n");
    printf("GL vendor: %s\n",                   glGetString(GL_VENDOR));
    printf("GL renderer: %s\n",                 glGetString(GL_RENDERER));
    printf("GL version: %s\n",                  glGetString(GL_VERSION));
    printf("GL extensions: %s\n",               glGetString(GL_EXTENSIONS));
    printf("GL shading language version: %s\n", glGetString(GL_SHADING_LANGUAGE_VERSION));

#define GET_LIMIT(limit) \
    { \
        GLint val{}; \
        glGetIntegerv(limit, &val); \
        printf(#limit ": %i\n", val); \
    }
    GET_LIMIT(GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);
    GET_LIMIT(GL_MAX_CUBE_MAP_TEXTURE_SIZE);
    GET_LIMIT(GL_MAX_FRAGMENT_UNIFORM_VECTORS)
    GET_LIMIT(GL_MAX_RENDERBUFFER_SIZE)
    GET_LIMIT(GL_MAX_TEXTURE_IMAGE_UNITS)
    GET_LIMIT(GL_MAX_TEXTURE_SIZE)
    GET_LIMIT(GL_MAX_VARYING_VECTORS)
    GET_LIMIT(GL_MAX_VERTEX_ATTRIBS)
    GET_LIMIT(GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS)
    GET_LIMIT(GL_MAX_VERTEX_UNIFORM_VECTORS)
    {
        GLint val[2] = {};
        glGetIntegerv(GL_MAX_VIEWPORT_DIMS, val);
        printf("GL_MAX_VIEWPORT_DIMS: %ix%i\n", val[0], val[1]);
    }
    {
        GLint val{};
        glGetIntegerv(GL_IMPLEMENTATION_COLOR_READ_FORMAT, &val);
        printf("GL_IMPLEMENTATION_COLOR_READ_FORMAT: 0x%x\n", val);
        glGetIntegerv(GL_IMPLEMENTATION_COLOR_READ_TYPE, &val);
        printf("GL_IMPLEMENTATION_COLOR_READ_TYPE: 0x%x\n", val);
    }

    GET_LIMIT(GL_NUM_COMPRESSED_TEXTURE_FORMATS)
    {
        GLint num{};
        glGetIntegerv(GL_NUM_COMPRESSED_TEXTURE_FORMATS, &num);
        GLint* formats = reinterpret_cast<GLint*>(malloc(sizeof(GLint) * num));
        glGetIntegerv(GL_COMPRESSED_TEXTURE_FORMATS, formats);
        for (int i=0; i<num; i++)
            printf("- 0x%x\n", formats[i]);
        free(formats);
    }
    
    GET_LIMIT(GL_NUM_SHADER_BINARY_FORMATS)
    {
        GLint num{};
        glGetIntegerv(GL_NUM_SHADER_BINARY_FORMATS, &num);
        GLint* formats = reinterpret_cast<GLint*>(malloc(sizeof(GLint) * num));
        glGetIntegerv(GL_SHADER_BINARY_FORMATS, formats);
        for (int i=0; i<num; i++)
            printf("- 0x%x\n", formats[i]);
        free(formats);
    }
}

#ifdef EGL_EXT_device_base

static EGLDisplay displayForDevice(EGLDeviceEXT device)
{
#ifdef EGL_EXT_platform_base
    PFNEGLGETPLATFORMDISPLAYEXTPROC eglGetPlatformDisplayExt = reinterpret_cast<PFNEGLGETPLATFORMDISPLAYEXTPROC>(eglGetProcAddress("eglGetPlatformDisplayEXT"));
    EGLint attribs[] = { EGL_NONE };
    EGLDisplay display = eglGetPlatformDisplayExt(EGL_PLATFORM_DEVICE_EXT, device, attribs);
    return display;
#else
#warning "Compiling without EGL_EXT_platform_base extension support!"
    return EGL_NO_DISPLAY;
#endif
}

static void printDevices()
{
    PFNEGLQUERYDEVICESEXTPROC eglQueryDevicesEXT = reinterpret_cast<PFNEGLQUERYDEVICESEXTPROC>(eglGetProcAddress("eglQueryDevicesEXT"));
    EGLDeviceEXT devices[32];
    EGLint num_devices;
    if (!eglQueryDevicesEXT(32, devices, &num_devices)) {
        printf("Failed to query devices.\n\n");
        return;
    }
    if (num_devices == 0) {
        printf("Found no devices.\n\n");
        return;
    }

    printf("Found %i device(s).",  num_devices);
    PFNEGLQUERYDEVICEATTRIBEXTPROC eglQueryDeviceAttribEXT = reinterpret_cast<PFNEGLQUERYDEVICEATTRIBEXTPROC>(eglGetProcAddress("eglQueryDeviceAttribEXT"));
    PFNEGLQUERYDEVICESTRINGEXTPROC eglQueryDeviceStringEXT = reinterpret_cast<PFNEGLQUERYDEVICESTRINGEXTPROC>(eglGetProcAddress("eglQueryDeviceStringEXT"));

    for (int i = 0; i < num_devices; ++i) {
        printf("Device %i:\n", i);
        EGLDeviceEXT device = devices[i];
        const char* devExts = eglQueryDeviceStringEXT(device, EGL_EXTENSIONS);
        if (devExts) {
            printf("  Device Extensions: ");
            if (strlen(devExts))
                printf("%s\n", devExts);
            else
                printf("none\n");
        } else {
            printf("  Failed to retrieve device extensions.\n");
        }

        for (int j = 0; j < devicePropertiesSize; ++j) {
            const auto property = deviceProperties[j];
            if (!devExts || strstr(devExts, property.extension) == nullptr)
                continue;
            switch (property.type) {
                case device_property_t::String:
                {
                    const char* value = eglQueryDeviceStringEXT(device, property.name);
                    printf("  %s: %i\n", property.displayName, value);
                    break;
                }
                case device_property_t::Attribute:
                {
                    EGLAttribKHR attrib;
                    if (eglQueryDeviceAttribEXT(device, property.name, &attrib) == EGL_FALSE)
                        break;
                    printf("  %s: %i\n", property.displayName, attrib);
                    break;
                }
            }
        }

        EGLDisplay display = displayForDevice(device);
        if (display == EGL_NO_DISPLAY) {
            printf("  No attached display.\n");
        }else {
            printf("  Device display:\n");
            printDisplay(display, "    ");
        }

        printf("\n");
    }
}
#endif

#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/Xatom.h>

static Display* x11Display = nullptr;
static Window   x11Window  = 0;

static bool createX11Window(int width, int height, bool hildon)
{
    x11Display = XOpenDisplay(nullptr);
    if (!x11Display) {
        fprintf(stderr, "Could not open X11 display!\n");
        return false;
    }

    Window rootWindow = DefaultRootWindow(x11Display);

    XSetWindowAttributes windowAttr = {};
    windowAttr.border_pixel = 0;
    windowAttr.event_mask   =
        PointerMotionMask | ButtonPressMask | ButtonReleaseMask |
        KeyPressMask | KeyReleaseMask | StructureNotifyMask;

    x11Window = XCreateWindow(
        x11Display, rootWindow,
        0, 0, width, height,
        0, CopyFromParent, InputOutput, CopyFromParent,
        CWBorderPixel | CWEventMask, &windowAttr);

    if (!x11Window) {
        fprintf(stderr, "Could not create X11 window!\n");
        return false;
    }
    x11Window_ = x11Window;

    if (hildon) {
        int one = 1;
        XChangeProperty(
            x11Display, x11Window,
            XInternAtom(x11Display, "_HILDON_NON_COMPOSITED_WINDOW", True),
            XA_INTEGER, 32, PropModeReplace,
            (unsigned char*)&one, 1);
    }

    XMapWindow(x11Display, x11Window);
    XSync(x11Display, False);
    return true;
}

static void destroyX11Window()
{
    if (x11Display && x11Window) {
        XUnmapWindow(x11Display, x11Window);
        XDestroyWindow(x11Display, x11Window);
    }
    if (x11Display) {
        XCloseDisplay(x11Display);
        x11Display = nullptr;
        x11Window  = 0;
    }
}

static bool recreateX11Window(int width, int height, EGLint visualId, bool hildon)
{
    if (x11Window) {
        XUnmapWindow(x11Display, x11Window);
        XDestroyWindow(x11Display, x11Window);
        x11Window  = 0;
        x11Window_ = 0;
    }

    XVisualInfo vi_template = {};
    vi_template.visualid    = (VisualID)visualId;
    int          n_visuals  = 0;
    XVisualInfo* vi         = XGetVisualInfo(x11Display, VisualIDMask, &vi_template, &n_visuals);
    if (!vi || n_visuals == 0) {
        fprintf(stderr, "No X11 visual found for EGL native visual ID %d\n", visualId);
        return false;
    }

    Window               rootWindow = DefaultRootWindow(x11Display);
    XSetWindowAttributes windowAttr = {};
    windowAttr.colormap    = XCreateColormap(x11Display, rootWindow, vi->visual, AllocNone);
    windowAttr.border_pixel = 0;
    windowAttr.event_mask  =
        PointerMotionMask | ButtonPressMask | ButtonReleaseMask |
        KeyPressMask | KeyReleaseMask | StructureNotifyMask;

    x11Window = XCreateWindow(
        x11Display, rootWindow,
        0, 0, width, height,
        0, vi->depth, InputOutput, vi->visual,
        CWColormap | CWBorderPixel | CWEventMask, &windowAttr);

    XFree(vi);

    if (!x11Window) {
        fprintf(stderr, "Could not recreate X11 window with EGL visual!\n");
        return false;
    }
    x11Window_ = x11Window;

    if (hildon) {
        int one = 1;
        XChangeProperty(
            x11Display, x11Window,
            XInternAtom(x11Display, "_HILDON_NON_COMPOSITED_WINDOW", True),
            XA_INTEGER, 32, PropModeReplace,
            (unsigned char*)&one, 1);
    }

    XMapWindow(x11Display, x11Window);
    XSync(x11Display, False);
    return true;
}
int main(int argc, char** argv)
{
    if (!createX11Window(128, 128, true)) {
        exit(1);
    }

    EGLDisplay display = eglGetDisplay((EGLNativeDisplayType)x11Display);
    if (display == EGL_NO_DISPLAY) {
        fprintf(stderr, "Could not obtain EGL display!\n");
        destroyX11Window();
        exit(1);
    }

    const char* clientExts = eglQueryString(display, EGL_EXTENSIONS);
    if (clientExts)
        printf("Client extensions: %s\n", clientExts);
    else
        printf("No client extensions.\n");

#ifdef EGL_EXT_device_base
    if (clientExts && strstr(clientExts, "EGL_EXT_device_base") != nullptr)
        printDevices();
#endif

    printf("Default display\n");
    printDisplay(display);
    destroyX11Window();
}
