#!/usr/bin/env python3

import os
import sys
import csv
import json
import argparse
from pathlib import Path
from collections import defaultdict
import statistics

def load_csv_results(csv_file):
    """Load JMeter CSV results and return parsed data."""
    results = []
    try:
        with open(csv_file, 'r') as f:
            reader = csv.DictReader(f)
            for row in reader:
                try:
                    # Convert numeric fields
                    row['elapsed'] = int(row['elapsed']) if row['elapsed'] else 0
                    row['latency'] = int(row['latency']) if row['latency'] else 0
                    row['Connect'] = int(row['Connect']) if row['Connect'] else 0
                    row['bytes'] = int(row['bytes']) if row['bytes'] else 0
                    row['sentBytes'] = int(row['sentBytes']) if row['sentBytes'] else 0
                    row['success'] = row['success'].lower() == 'true'
                    row['responseCode'] = int(row['responseCode']) if row['responseCode'].isdigit() else 0
                    results.append(row)
                except (ValueError, KeyError) as e:
                    print(f"Warning: Skipping malformed row: {e}")
                    continue
    except FileNotFoundError:
        print(f"Error: CSV file not found: {csv_file}")
        return []
    except Exception as e:
        print(f"Error reading CSV file: {e}")
        return []
    
    return results

def calculate_statistics(results):
    """Calculate key performance statistics from results."""
    if not results:
        return {}
    
    # Response times
    response_times = [r['elapsed'] for r in results]
    success_count = sum(1 for r in results if r['success'])
    error_count = len(results) - success_count
    
    # Calculate percentiles
    response_times_sorted = sorted(response_times)
    total_requests = len(response_times)
    
    def percentile(data, p):
        if not data:
            return 0
        k = (len(data) - 1) * p / 100.0
        f = int(k)
        c = k - f
        if f == len(data) - 1:
            return data[f]
        return data[f] * (1 - c) + data[f + 1] * c
    
    # Calculate throughput (requests per second)
    if results:
        timestamps = [int(r['timeStamp']) for r in results]
        duration = (max(timestamps) - min(timestamps)) / 1000.0  # Convert to seconds
        throughput = len(results) / duration if duration > 0 else 0
    else:
        throughput = 0
    
    stats = {
        'total_requests': total_requests,
        'successful_requests': success_count,
        'failed_requests': error_count,
        'success_rate': (success_count / total_requests * 100) if total_requests > 0 else 0,
        'error_rate': (error_count / total_requests * 100) if total_requests > 0 else 0,
        'avg_response_time': statistics.mean(response_times) if response_times else 0,
        'median_response_time': statistics.median(response_times) if response_times else 0,
        'min_response_time': min(response_times) if response_times else 0,
        'max_response_time': max(response_times) if response_times else 0,
        'p90_response_time': percentile(response_times_sorted, 90),
        'p95_response_time': percentile(response_times_sorted, 95),
        'p99_response_time': percentile(response_times_sorted, 99),
        'throughput': throughput,
        'std_dev_response_time': statistics.stdev(response_times) if len(response_times) > 1 else 0
    }
    
    return stats

def analyze_scaling_results(results_dir):
    """Analyze all scaling test results and identify breaking points."""
    scaling_results = []
    results_path = Path(results_dir)
    
    if not results_path.exists():
        print(f"Error: Results directory not found: {results_dir}")
        return []
    
    # Find all result directories
    for result_dir in results_path.glob("results-analytics-test-*"):
        # Extract test parameters from directory name
        dir_name = result_dir.name
        # Format: results-analytics-test-{threads}t-{events_per_second}eps
        try:
            parts = dir_name.split('-')
            threads_part = next(p for p in parts if p.endswith('t'))
            eps_part = next(p for p in parts if p.endswith('eps'))
            
            threads = int(threads_part[:-1])  # Remove 't' suffix
            events_per_second = int(eps_part[:-3])  # Remove 'eps' suffix
            
            # Find CSV file in result directory
            csv_files = list(result_dir.glob("*.csv"))
            if csv_files:
                csv_file = csv_files[0]  # Take the first CSV file found
                results = load_csv_results(csv_file)
                stats = calculate_statistics(results)
                
                if stats:
                    stats['threads'] = threads
                    stats['target_events_per_second'] = events_per_second
                    stats['test_config'] = f"{threads}t-{events_per_second}eps"
                    scaling_results.append(stats)
                    
        except (ValueError, StopIteration) as e:
            print(f"Warning: Could not parse test configuration from {dir_name}: {e}")
            continue
    
    # Sort by number of threads
    scaling_results.sort(key=lambda x: x['threads'])
    return scaling_results

def identify_breaking_points(scaling_results):
    """Identify performance breaking points in scaling results."""
    if len(scaling_results) < 2:
        return {}
    
    breaking_points = {}
    
    # Thresholds for identifying breaking points
    RESPONSE_TIME_THRESHOLD = 2.0  # 2x increase
    ERROR_RATE_THRESHOLD = 5.0     # 5% error rate
    THROUGHPUT_DECLINE_THRESHOLD = 0.8  # 20% decline in throughput efficiency
    
    previous = scaling_results[0]
    
    for current in scaling_results[1:]:
        # Check response time degradation
        if (previous['avg_response_time'] > 0 and 
            current['avg_response_time'] / previous['avg_response_time'] > RESPONSE_TIME_THRESHOLD):
            if 'response_time_degradation' not in breaking_points:
                breaking_points['response_time_degradation'] = {
                    'threads': current['threads'],
                    'config': current['test_config'],
                    'avg_response_time': current['avg_response_time'],
                    'previous_avg_response_time': previous['avg_response_time'],
                    'degradation_factor': current['avg_response_time'] / previous['avg_response_time']
                }
        
        # Check error rate increase
        if current['error_rate'] > ERROR_RATE_THRESHOLD:
            if 'error_rate_threshold' not in breaking_points:
                breaking_points['error_rate_threshold'] = {
                    'threads': current['threads'],
                    'config': current['test_config'],
                    'error_rate': current['error_rate'],
                    'failed_requests': current['failed_requests']
                }
        
        # Check throughput efficiency decline
        expected_throughput = current['target_events_per_second']
        actual_throughput = current['throughput']
        efficiency = actual_throughput / expected_throughput if expected_throughput > 0 else 0
        
        if efficiency < THROUGHPUT_DECLINE_THRESHOLD:
            if 'throughput_efficiency' not in breaking_points:
                breaking_points['throughput_efficiency'] = {
                    'threads': current['threads'],
                    'config': current['test_config'],
                    'expected_throughput': expected_throughput,
                    'actual_throughput': actual_throughput,
                    'efficiency': efficiency
                }
        
        previous = current
    
    return breaking_points

def print_scaling_analysis(scaling_results, breaking_points):
    """Print comprehensive scaling analysis."""
    print("\n" + "="*80)
    print("ANALYTICS EVENTS SCALING TEST ANALYSIS")
    print("="*80)
    
    if not scaling_results:
        print("No scaling results found.")
        return
    
    # Summary table
    print(f"\n{'Config':<15} {'Threads':<8} {'Target EPS':<11} {'Actual EPS':<11} {'Avg RT (ms)':<12} {'P95 RT (ms)':<12} {'Error %':<8} {'Success %':<9}")
    print("-" * 100)
    
    for result in scaling_results:
        print(f"{result['test_config']:<15} "
              f"{result['threads']:<8} "
              f"{result['target_events_per_second']:<11.1f} "
              f"{result['throughput']:<11.1f} "
              f"{result['avg_response_time']:<12.1f} "
              f"{result['p95_response_time']:<12.1f} "
              f"{result['error_rate']:<8.1f} "
              f"{result['success_rate']:<9.1f}")
    
    # Breaking points analysis
    print(f"\n{'='*50}")
    print("BREAKING POINTS ANALYSIS")
    print("="*50)
    
    if not breaking_points:
        print("No significant breaking points detected.")
    else:
        if 'response_time_degradation' in breaking_points:
            bp = breaking_points['response_time_degradation']
            print(f"\n🔴 RESPONSE TIME DEGRADATION:")
            print(f"   Breaking point at: {bp['config']} ({bp['threads']} threads)")
            print(f"   Response time increased {bp['degradation_factor']:.1f}x")
            print(f"   From {bp['previous_avg_response_time']:.1f}ms to {bp['avg_response_time']:.1f}ms")
        
        if 'error_rate_threshold' in breaking_points:
            bp = breaking_points['error_rate_threshold']
            print(f"\n🔴 ERROR RATE THRESHOLD:")
            print(f"   Breaking point at: {bp['config']} ({bp['threads']} threads)")
            print(f"   Error rate: {bp['error_rate']:.1f}%")
            print(f"   Failed requests: {bp['failed_requests']}")
        
        if 'throughput_efficiency' in breaking_points:
            bp = breaking_points['throughput_efficiency']
            print(f"\n🔴 THROUGHPUT EFFICIENCY:")
            print(f"   Breaking point at: {bp['config']} ({bp['threads']} threads)")
            print(f"   Expected: {bp['expected_throughput']:.1f} EPS")
            print(f"   Actual: {bp['actual_throughput']:.1f} EPS")
            print(f"   Efficiency: {bp['efficiency']:.1%}")
    
    # Recommendations
    print(f"\n{'='*50}")
    print("RECOMMENDATIONS")
    print("="*50)
    
    # Find optimal configuration (before breaking points)
    optimal_config = None
    for result in reversed(scaling_results):
        if (result['error_rate'] < 1.0 and 
            result['avg_response_time'] < 1000 and  # Less than 1 second
            result['success_rate'] > 99.0):
            optimal_config = result
            break
    
    if optimal_config:
        print(f"\n✅ RECOMMENDED OPTIMAL CONFIGURATION:")
        print(f"   Config: {optimal_config['test_config']}")
        print(f"   Threads: {optimal_config['threads']}")
        print(f"   Target throughput: {optimal_config['target_events_per_second']} EPS")
        print(f"   Actual throughput: {optimal_config['throughput']:.1f} EPS")
        print(f"   Average response time: {optimal_config['avg_response_time']:.1f}ms")
        print(f"   95th percentile response time: {optimal_config['p95_response_time']:.1f}ms")
        print(f"   Success rate: {optimal_config['success_rate']:.1f}%")
    
    # Maximum sustainable load
    max_load = max(scaling_results, key=lambda x: x['throughput'])
    print(f"\n📊 MAXIMUM OBSERVED THROUGHPUT:")
    print(f"   Config: {max_load['test_config']}")
    print(f"   Peak throughput: {max_load['throughput']:.1f} EPS")
    print(f"   Response time at peak: {max_load['avg_response_time']:.1f}ms")
    print(f"   Error rate at peak: {max_load['error_rate']:.1f}%")

def main():
    parser = argparse.ArgumentParser(description='Analyze JMeter analytics events test results')
    parser.add_argument('--results-dir', default='target/scaling-results', 
                       help='Directory containing scaling test results')
    parser.add_argument('--single-result', help='Analyze a single CSV result file')
    parser.add_argument('--output-json', help='Output results to JSON file')
    
    args = parser.parse_args()
    
    if args.single_result:
        # Analyze single result file
        results = load_csv_results(args.single_result)
        stats = calculate_statistics(results)
        
        if stats:
            print("\nSINGLE TEST ANALYSIS")
            print("="*50)
            print(f"Total requests: {stats['total_requests']}")
            print(f"Successful requests: {stats['successful_requests']}")
            print(f"Failed requests: {stats['failed_requests']}")
            print(f"Success rate: {stats['success_rate']:.1f}%")
            print(f"Error rate: {stats['error_rate']:.1f}%")
            print(f"Average response time: {stats['avg_response_time']:.1f}ms")
            print(f"Median response time: {stats['median_response_time']:.1f}ms")
            print(f"95th percentile: {stats['p95_response_time']:.1f}ms")
            print(f"99th percentile: {stats['p99_response_time']:.1f}ms")
            print(f"Throughput: {stats['throughput']:.1f} requests/second")
        else:
            print("No valid data found in the result file.")
    else:
        # Analyze scaling results
        scaling_results = analyze_scaling_results(args.results_dir)
        breaking_points = identify_breaking_points(scaling_results)
        print_scaling_analysis(scaling_results, breaking_points)
        
        # Output to JSON if requested
        if args.output_json:
            output_data = {
                'scaling_results': scaling_results,
                'breaking_points': breaking_points,
                'analysis_timestamp': str(Path().cwd())
            }
            with open(args.output_json, 'w') as f:
                json.dump(output_data, f, indent=2)
            print(f"\nResults saved to: {args.output_json}")

if __name__ == '__main__':
    main() 