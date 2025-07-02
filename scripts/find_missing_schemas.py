#!/usr/bin/env python3

import os
import re
import sys

def find_rest_resources_with_response_entity_view():
    """Find all REST resource files that use ResponseEntityView"""
    base_path = "/Users/stevebolton/git/core-baseline/dotCMS/src/main/java"
    
    # Find files with @Path annotation
    rest_files = []
    for root, dirs, files in os.walk(base_path):
        for file in files:
            if file.endswith('.java'):
                filepath = os.path.join(root, file)
                try:
                    with open(filepath, 'r', encoding='utf-8') as f:
                        content = f.read()
                        if '@Path' in content and 'ResponseEntityView' in content:
                            rest_files.append(filepath)
                except Exception as e:
                    print(f"Error reading {filepath}: {e}")
    
    return rest_files

def analyze_file(filepath):
    """Analyze a single file for ResponseEntityView usage and missing schemas"""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
            lines = content.split('\n')
    except Exception as e:
        return None, f"Error reading file: {e}"
    
    results = []
    
    # Find methods that return ResponseEntityView
    response_entity_lines = []
    for i, line in enumerate(lines):
        if 'Response.ok(new ResponseEntityView' in line:
            response_entity_lines.append(i + 1)  # 1-based line numbers
    
    # For each ResponseEntityView usage, find the corresponding method
    for line_num in response_entity_lines:
        method_info = find_method_for_line(lines, line_num - 1)  # Convert to 0-based
        if method_info:
            # Check if this method has proper @ApiResponse with schema
            api_response_info = check_api_response_schema(lines, method_info['start_line'])
            
            results.append({
                'line_num': line_num,
                'method_name': method_info['method_name'],
                'method_line': method_info['start_line'] + 1,
                'has_api_response': api_response_info['has_api_response'],
                'has_content_spec': api_response_info['has_content_spec'],
                'has_schema_spec': api_response_info['has_schema_spec'],
                'needs_update': not api_response_info['has_schema_spec']
            })
    
    return results, None

def find_method_for_line(lines, target_line):
    """Find the method signature that contains the target line"""
    # Search backwards from target line to find method signature
    for i in range(target_line, -1, -1):
        line = lines[i].strip()
        if line.startswith('public Response '):
            method_match = re.search(r'public Response (\w+)\s*\(', line)
            if method_match:
                return {
                    'method_name': method_match.group(1),
                    'start_line': i
                }
    return None

def check_api_response_schema(lines, method_start_line):
    """Check if method has proper @ApiResponse with schema specification"""
    # Look backwards from method to find @ApiResponse annotations
    has_api_response = False
    has_content_spec = False
    has_schema_spec = False
    
    # Search up to 50 lines before the method for annotations
    search_start = max(0, method_start_line - 50)
    for i in range(method_start_line, search_start, -1):
        line = lines[i].strip()
        
        if '@ApiResponse' in line:
            has_api_response = True
            
            # Check this and next few lines for content specification
            for j in range(i, min(len(lines), i + 10)):
                check_line = lines[j].strip()
                if 'content = @Content' in check_line:
                    has_content_spec = True
                    # Look for schema specification
                    for k in range(j, min(len(lines), j + 5)):
                        schema_line = lines[k].strip()
                        if 'schema = @Schema' in schema_line or '@io.swagger.v3.oas.annotations.media.Schema' in schema_line:
                            has_schema_spec = True
                            break
                    break
        
        # Stop searching if we hit another method or class declaration
        if line.startswith('public ') and 'Response' in line and i != method_start_line:
            break
    
    return {
        'has_api_response': has_api_response,
        'has_content_spec': has_content_spec,
        'has_schema_spec': has_schema_spec
    }

def main():
    print("=== REST Resources with ResponseEntityView Missing Schema Analysis ===\n")
    
    files = find_rest_resources_with_response_entity_view()
    print(f"Found {len(files)} REST resource files using ResponseEntityView\n")
    
    total_methods = 0
    methods_needing_update = 0
    
    for filepath in files:
        relative_path = filepath.replace('/Users/stevebolton/git/core-baseline/', '')
        results, error = analyze_file(filepath)
        
        if error:
            print(f"ERROR analyzing {relative_path}: {error}\n")
            continue
        
        if not results:
            continue
            
        print(f"=== {relative_path} ===")
        
        methods_in_file = len(results)
        methods_needing_update_in_file = sum(1 for r in results if r['needs_update'])
        
        total_methods += methods_in_file
        methods_needing_update += methods_needing_update_in_file
        
        print(f"Methods returning ResponseEntityView: {methods_in_file}")
        print(f"Methods needing schema updates: {methods_needing_update_in_file}")
        
        if methods_needing_update_in_file > 0:
            print("\nMethods needing updates:")
            for result in results:
                if result['needs_update']:
                    print(f"  - {result['method_name']} (line {result['method_line']}) -> ResponseEntityView usage at line {result['line_num']}")
                    print(f"    Has @ApiResponse: {result['has_api_response']}")
                    print(f"    Has content spec: {result['has_content_spec']}")
                    print(f"    Has schema spec: {result['has_schema_spec']}")
        
        print("")
    
    print(f"=== SUMMARY ===")
    print(f"Total files analyzed: {len(files)}")
    print(f"Total methods returning ResponseEntityView: {total_methods}")
    print(f"Methods needing schema updates: {methods_needing_update}")
    print(f"Completion percentage: {((total_methods - methods_needing_update) / total_methods * 100):.1f}%")

if __name__ == "__main__":
    main()