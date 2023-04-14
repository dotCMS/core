/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.license.bouncycastle.asn1;

public interface DERTags
{
    public static final int BOOLEAN             = 0x01;
    public static final int INTEGER             = 0x02;
    public static final int BIT_STRING          = 0x03;
    public static final int OCTET_STRING        = 0x04;
    public static final int NULL                = 0x05;
    public static final int OBJECT_IDENTIFIER   = 0x06;
    public static final int EXTERNAL            = 0x08;
    public static final int ENUMERATED          = 0x0a;
    public static final int SEQUENCE            = 0x10;
    public static final int SEQUENCE_OF         = 0x10; // for completeness
    public static final int SET                 = 0x11;
    public static final int SET_OF              = 0x11; // for completeness


    public static final int NUMERIC_STRING      = 0x12;
    public static final int PRINTABLE_STRING    = 0x13;
    public static final int T61_STRING          = 0x14;
    public static final int VIDEOTEX_STRING     = 0x15;
    public static final int IA5_STRING          = 0x16;
    public static final int UTC_TIME            = 0x17;
    public static final int GENERALIZED_TIME    = 0x18;
    public static final int GRAPHIC_STRING      = 0x19;
    public static final int VISIBLE_STRING      = 0x1a;
    public static final int GENERAL_STRING      = 0x1b;
    public static final int UNIVERSAL_STRING    = 0x1c;
    public static final int BMP_STRING          = 0x1e;
    public static final int UTF8_STRING         = 0x0c;
    
    public static final int CONSTRUCTED         = 0x20;
    public static final int APPLICATION         = 0x40;
    public static final int TAGGED              = 0x80;
}
