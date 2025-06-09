#!/usr/bin/env python3

import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import sys
import os
from matplotlib.patches import Rectangle

def create_performance_graphs(csv_file):
    """Create comprehensive performance comparison graphs"""
    
    # Read the data
    try:
        df = pd.read_csv(csv_file)
    except FileNotFoundError:
        print(f"Error: Could not find CSV file: {csv_file}")
        return
    
    # Create output directory
    output_dir = os.path.dirname(csv_file)
    graphs_dir = os.path.join(output_dir, "graphs")
    os.makedirs(graphs_dir, exist_ok=True)
    
    # Set style
    plt.style.use('seaborn-v0_8')
    sns.set_palette("husl")
    
    # Create figure with subplots
    fig = plt.figure(figsize=(20, 16))
    
    # 1. Throughput Comparison (Actual EPS vs Target EPS)
    ax1 = plt.subplot(2, 3, 1)
    for endpoint in df['endpoint'].unique():
        data = df[df['endpoint'] == endpoint]
        plt.plot(data['target_eps'], data['actual_eps'], 
                marker='o', linewidth=2, markersize=6, 
                label=f'{endpoint.upper()} API')
    
    # Add perfect scaling line
    max_target = df['target_eps'].max()
    plt.plot([0, max_target], [0, max_target], 
             'k--', alpha=0.5, label='Perfect Scaling')
    
    plt.xlabel('Target Events/Second')
    plt.ylabel('Actual Events/Second')
    plt.title('Throughput: Actual vs Target Performance')
    plt.legend()
    plt.grid(True, alpha=0.3)
    
    # 2. Response Time vs Throughput
    ax2 = plt.subplot(2, 3, 2)
    for endpoint in df['endpoint'].unique():
        data = df[df['endpoint'] == endpoint]
        plt.plot(data['actual_eps'], data['avg_response_ms'], 
                marker='s', linewidth=2, markersize=6,
                label=f'{endpoint.upper()} API')
    
    plt.xlabel('Actual Events/Second')
    plt.ylabel('Average Response Time (ms)')
    plt.title('Response Time vs Throughput')
    plt.legend()
    plt.grid(True, alpha=0.3)
    plt.yscale('log')  # Log scale for response time
    
    # 3. Error Rate vs Throughput
    ax3 = plt.subplot(2, 3, 3)
    for endpoint in df['endpoint'].unique():
        data = df[df['endpoint'] == endpoint]
        plt.plot(data['actual_eps'], data['error_rate_percent'], 
                marker='^', linewidth=2, markersize=6,
                label=f'{endpoint.upper()} API')
    
    plt.xlabel('Actual Events/Second')
    plt.ylabel('Error Rate (%)')
    plt.title('Error Rate vs Throughput')
    plt.legend()
    plt.grid(True, alpha=0.3)
    
    # Add failure threshold line
    plt.axhline(y=50, color='red', linestyle='--', alpha=0.7, label='Failure Threshold (50%)')
    
    # 4. Success Rate vs Throughput
    ax4 = plt.subplot(2, 3, 4)
    for endpoint in df['endpoint'].unique():
        data = df[df['endpoint'] == endpoint]
        plt.plot(data['actual_eps'], data['success_rate_percent'], 
                marker='D', linewidth=2, markersize=6,
                label=f'{endpoint.upper()} API')
    
    plt.xlabel('Actual Events/Second')
    plt.ylabel('Success Rate (%)')
    plt.title('Success Rate vs Throughput')
    plt.legend()
    plt.grid(True, alpha=0.3)
    plt.ylim(0, 105)
    
    # 5. Response Time Distribution (Box plot style)
    ax5 = plt.subplot(2, 3, 5)
    
    # Create response time range visualization
    for i, endpoint in enumerate(df['endpoint'].unique()):
        data = df[df['endpoint'] == endpoint]
        y_pos = [i] * len(data)
        
        # Plot min to max range as error bars
        plt.errorbar(data['avg_response_ms'], y_pos, 
                    xerr=[data['avg_response_ms'] - data['min_response_ms'], 
                          data['max_response_ms'] - data['avg_response_ms']], 
                    fmt='o', capsize=5, capthick=2, 
                    label=f'{endpoint.upper()} API')
    
    plt.xlabel('Response Time (ms)')
    plt.ylabel('Endpoint')
    plt.title('Response Time Distribution (Min-Avg-Max)')
    plt.yticks(range(len(df['endpoint'].unique())), 
               [ep.upper() + ' API' for ep in df['endpoint'].unique()])
    plt.grid(True, alpha=0.3)
    plt.xscale('log')
    
    # 6. Performance Summary Table
    ax6 = plt.subplot(2, 3, 6)
    ax6.axis('tight')
    ax6.axis('off')
    
    # Create summary table
    summary_data = []
    for endpoint in df['endpoint'].unique():
        data = df[df['endpoint'] == endpoint]
        
        # Find best performance point (highest throughput with >95% success)
        good_performance = data[data['success_rate_percent'] > 95]
        if len(good_performance) > 0:
            best_point = good_performance.loc[good_performance['actual_eps'].idxmax()]
            max_throughput = best_point['actual_eps']
            max_response = best_point['avg_response_ms']
        else:
            max_throughput = data['actual_eps'].max()
            max_response = data.loc[data['actual_eps'].idxmax(), 'avg_response_ms']
        
        # Find failure point
        failure_point = data[data['success_rate_percent'] < 50]
        if len(failure_point) > 0:
            failure_eps = failure_point['actual_eps'].min()
        else:
            failure_eps = "Not reached"
        
        summary_data.append([
            endpoint.upper() + ' API',
            f"{max_throughput:.1f}",
            f"{max_response:.0f}ms",
            f"{failure_eps}" if failure_eps != "Not reached" else "Not reached"
        ])
    
    table = ax6.table(cellText=summary_data,
                     colLabels=['Endpoint', 'Max Reliable\nThroughput (eps)', 
                               'Response Time\nat Max', 'Failure Point\n(eps)'],
                     cellLoc='center',
                     loc='center')
    table.auto_set_font_size(False)
    table.set_fontsize(10)
    table.scale(1.2, 1.5)
    
    # Style the table
    for i in range(len(summary_data) + 1):
        for j in range(4):
            if i == 0:  # Header
                table[(i, j)].set_facecolor('#4CAF50')
                table[(i, j)].set_text_props(weight='bold', color='white')
            else:
                table[(i, j)].set_facecolor('#f8f9fa')
    
    plt.title('Performance Summary', pad=20, fontsize=14, fontweight='bold')
    
    plt.tight_layout()
    
    # Save the comprehensive graph
    output_file = os.path.join(graphs_dir, 'performance_comparison.png')
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    print(f"Comprehensive graph saved to: {output_file}")
    
    # Create separate detailed graphs
    create_detailed_graphs(df, graphs_dir)
    
    plt.show()

def create_detailed_graphs(df, output_dir):
    """Create individual detailed graphs"""
    
    # 1. Detailed Throughput Analysis
    plt.figure(figsize=(12, 8))
    
    for endpoint in df['endpoint'].unique():
        data = df[df['endpoint'] == endpoint]
        
        # Plot actual vs target
        plt.subplot(2, 1, 1)
        plt.plot(data['target_eps'], data['actual_eps'], 
                marker='o', linewidth=2, markersize=8, 
                label=f'{endpoint.upper()} API')
        
        # Plot efficiency (actual/target ratio)
        plt.subplot(2, 1, 2)
        efficiency = (data['actual_eps'] / data['target_eps']) * 100
        plt.plot(data['target_eps'], efficiency, 
                marker='s', linewidth=2, markersize=8,
                label=f'{endpoint.upper()} API')
    
    plt.subplot(2, 1, 1)
    max_target = df['target_eps'].max()
    plt.plot([0, max_target], [0, max_target], 'k--', alpha=0.5, label='Perfect Scaling')
    plt.xlabel('Target Events/Second')
    plt.ylabel('Actual Events/Second')
    plt.title('Throughput Performance Analysis')
    plt.legend()
    plt.grid(True, alpha=0.3)
    
    plt.subplot(2, 1, 2)
    plt.axhline(y=100, color='green', linestyle='--', alpha=0.7, label='100% Efficiency')
    plt.axhline(y=80, color='orange', linestyle='--', alpha=0.7, label='80% Efficiency')
    plt.xlabel('Target Events/Second')
    plt.ylabel('Efficiency (%)')
    plt.title('Throughput Efficiency (Actual/Target %)')
    plt.legend()
    plt.grid(True, alpha=0.3)
    
    plt.tight_layout()
    plt.savefig(os.path.join(output_dir, 'throughput_analysis.png'), dpi=300, bbox_inches='tight')
    plt.close()
    
    # 2. Failure Analysis
    plt.figure(figsize=(12, 6))
    
    plt.subplot(1, 2, 1)
    for endpoint in df['endpoint'].unique():
        data = df[df['endpoint'] == endpoint]
        plt.scatter(data['actual_eps'], data['error_rate_percent'], 
                   s=100, alpha=0.7, label=f'{endpoint.upper()} API')
    
    plt.axhline(y=50, color='red', linestyle='--', alpha=0.7, label='Failure Threshold')
    plt.xlabel('Actual Events/Second')
    plt.ylabel('Error Rate (%)')
    plt.title('Error Rate vs Throughput')
    plt.legend()
    plt.grid(True, alpha=0.3)
    
    plt.subplot(1, 2, 2)
    for endpoint in df['endpoint'].unique():
        data = df[df['endpoint'] == endpoint]
        plt.scatter(data['actual_eps'], data['success_rate_percent'], 
                   s=100, alpha=0.7, label=f'{endpoint.upper()} API')
    
    plt.axhline(y=95, color='green', linestyle='--', alpha=0.7, label='Reliability Target (95%)')
    plt.axhline(y=50, color='red', linestyle='--', alpha=0.7, label='Failure Threshold (50%)')
    plt.xlabel('Actual Events/Second')
    plt.ylabel('Success Rate (%)')
    plt.title('Success Rate vs Throughput')
    plt.legend()
    plt.grid(True, alpha=0.3)
    
    plt.tight_layout()
    plt.savefig(os.path.join(output_dir, 'failure_analysis.png'), dpi=300, bbox_inches='tight')
    plt.close()
    
    print(f"Detailed graphs saved to: {output_dir}")

def generate_report(csv_file):
    """Generate a text report with key findings"""
    df = pd.read_csv(csv_file)
    
    output_dir = os.path.dirname(csv_file)
    report_file = os.path.join(output_dir, 'performance_report.txt')
    
    with open(report_file, 'w') as f:
        f.write("PERFORMANCE LIMITS ANALYSIS REPORT\n")
        f.write("=" * 50 + "\n\n")
        
        for endpoint in df['endpoint'].unique():
            data = df[df['endpoint'] == endpoint]
            
            f.write(f"{endpoint.upper()} API ANALYSIS:\n")
            f.write("-" * 30 + "\n")
            
            # Find maximum reliable throughput (>95% success rate)
            reliable_data = data[data['success_rate_percent'] > 95]
            if len(reliable_data) > 0:
                max_reliable = reliable_data.loc[reliable_data['actual_eps'].idxmax()]
                f.write(f"Maximum Reliable Throughput: {max_reliable['actual_eps']:.1f} eps\n")
                f.write(f"Response Time at Max Reliable: {max_reliable['avg_response_ms']:.0f}ms\n")
            
            # Find absolute maximum
            max_point = data.loc[data['actual_eps'].idxmax()]
            f.write(f"Absolute Maximum Throughput: {max_point['actual_eps']:.1f} eps\n")
            f.write(f"Response Time at Absolute Max: {max_point['avg_response_ms']:.0f}ms\n")
            f.write(f"Success Rate at Absolute Max: {max_point['success_rate_percent']:.1f}%\n")
            
            # Find failure point
            failure_data = data[data['success_rate_percent'] < 50]
            if len(failure_data) > 0:
                failure_point = failure_data.loc[failure_data['actual_eps'].idxmin()]
                f.write(f"Failure Point: {failure_point['actual_eps']:.1f} eps\n")
                f.write(f"Success Rate at Failure: {failure_point['success_rate_percent']:.1f}%\n")
            else:
                f.write("Failure Point: Not reached in testing\n")
            
            f.write("\n")
        
        # Comparison
        f.write("COMPARATIVE ANALYSIS:\n")
        f.write("-" * 30 + "\n")
        
        dotcms_data = df[df['endpoint'] == 'dotcms']
        direct_data = df[df['endpoint'] == 'direct']
        
        if len(dotcms_data) > 0 and len(direct_data) > 0:
            dotcms_max = dotcms_data['actual_eps'].max()
            direct_max = direct_data['actual_eps'].max()
            
            throughput_ratio = direct_max / dotcms_max
            f.write(f"Direct Analytics vs DotCMS API throughput ratio: {throughput_ratio:.1f}x\n")
            
            # Response time comparison at similar loads
            f.write("\nResponse Time Comparison at Similar Loads:\n")
            for target in [100, 200, 300]:
                dotcms_near = dotcms_data[abs(dotcms_data['actual_eps'] - target) < 20]
                direct_near = direct_data[abs(direct_data['actual_eps'] - target) < 50]
                
                if len(dotcms_near) > 0 and len(direct_near) > 0:
                    dotcms_resp = dotcms_near['avg_response_ms'].iloc[0]
                    direct_resp = direct_near['avg_response_ms'].iloc[0]
                    f.write(f"At ~{target} eps: DotCMS {dotcms_resp:.0f}ms vs Direct {direct_resp:.0f}ms\n")
    
    print(f"Performance report saved to: {report_file}")

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python generate_performance_graphs.py <csv_file>")
        sys.exit(1)
    
    csv_file = sys.argv[1]
    print(f"Generating performance graphs from: {csv_file}")
    
    create_performance_graphs(csv_file)
    generate_report(csv_file)
    
    print("\nAnalysis complete!")
    print("Files generated:")
    print("- performance_comparison.png (comprehensive overview)")
    print("- throughput_analysis.png (detailed throughput)")
    print("- failure_analysis.png (error/success rates)")
    print("- performance_report.txt (summary report)") 