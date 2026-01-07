#!/usr/bin/env python3
"""
Example: Python microservice that validates dotCMS service JWTs.

This shows how to implement a receiving service (like wa11y accessibility checker)
that validates JWTs from dotCMS.

Requirements:
    pip install flask pyjwt

Environment variables:
    JWT_SIGNING_KEY: The shared signing key (from K8s secret)
    SERVICE_ID: This service's identifier (e.g., "wa11y-checker")
"""

import os
import functools
from datetime import datetime
from flask import Flask, request, jsonify, g
import jwt

app = Flask(__name__)

# Configuration from environment
SIGNING_KEY = os.environ.get('JWT_SIGNING_KEY')
SERVICE_ID = os.environ.get('SERVICE_ID', 'wa11y-checker')

if not SIGNING_KEY:
    raise RuntimeError("JWT_SIGNING_KEY environment variable is required")


def require_service_auth(f):
    """
    Decorator to require valid service JWT authentication.

    Usage:
        @app.route('/api/check')
        @require_service_auth
        def check_accessibility():
            # Access validated claims via g.service_auth
            print(f"Request from: {g.service_auth['source_cluster']}")
    """
    @functools.wraps(f)
    def decorated(*args, **kwargs):
        auth_header = request.headers.get('Authorization', '')

        if not auth_header.startswith('Bearer '):
            return jsonify({
                'error': 'Missing or invalid Authorization header',
                'hint': 'Include header: Authorization: Bearer <jwt-token>'
            }), 401

        token = auth_header[7:]  # Remove 'Bearer ' prefix

        try:
            # Decode and validate the JWT
            payload = jwt.decode(
                token,
                SIGNING_KEY,
                algorithms=['HS256', 'HS384', 'HS512'],
                audience=SERVICE_ID,
                options={
                    'require': ['exp', 'iat', 'aud', 'iss'],
                    'verify_exp': True,
                    'verify_iat': True,
                    'verify_aud': True,
                }
            )

            # Store validated claims for use in the endpoint
            g.service_auth = {
                'token_id': payload.get('jti'),
                'service_id': payload.get('sid'),
                'source_cluster': payload.get('src'),
                'issuer': payload.get('iss'),
                'audience': payload.get('aud'),
                'issued_at': datetime.fromtimestamp(payload.get('iat', 0)),
                'expires_at': datetime.fromtimestamp(payload.get('exp', 0)),
            }

            app.logger.info(
                f"Authenticated request from cluster={g.service_auth['source_cluster']}, "
                f"service={g.service_auth['service_id']}"
            )

            return f(*args, **kwargs)

        except jwt.ExpiredSignatureError:
            app.logger.warning(f"Expired token from {request.remote_addr}")
            return jsonify({'error': 'Token has expired'}), 401

        except jwt.InvalidAudienceError:
            app.logger.warning(
                f"Invalid audience in token from {request.remote_addr}"
            )
            return jsonify({
                'error': 'Invalid audience',
                'expected': SERVICE_ID
            }), 401

        except jwt.InvalidSignatureError:
            app.logger.warning(f"Invalid signature from {request.remote_addr}")
            return jsonify({'error': 'Invalid token signature'}), 401

        except jwt.DecodeError as e:
            app.logger.warning(f"Malformed token from {request.remote_addr}: {e}")
            return jsonify({'error': 'Malformed token'}), 401

        except Exception as e:
            app.logger.error(f"Unexpected auth error: {e}")
            return jsonify({'error': 'Authentication failed'}), 401

    return decorated


# ============================================================================
# Example Endpoints
# ============================================================================

@app.route('/health')
def health_check():
    """Health check endpoint - no authentication required."""
    return jsonify({
        'status': 'ok',
        'service': SERVICE_ID,
        'timestamp': datetime.utcnow().isoformat()
    })


@app.route('/api/v1/check', methods=['POST'])
@require_service_auth
def check_accessibility():
    """
    Check a page for accessibility issues.
    Requires valid service JWT.
    """
    data = request.get_json()

    if not data or 'url' not in data:
        return jsonify({'error': 'url is required'}), 400

    url = data['url']
    standard = data.get('standard', 'WCAG2AA')
    timeout = data.get('timeout', 30000)

    app.logger.info(
        f"Checking accessibility for {url} "
        f"(requested by {g.service_auth['source_cluster']})"
    )

    # Simulate accessibility check results
    # In production, this would call a real accessibility checker
    results = {
        'url': url,
        'standard': standard,
        'score': 85,
        'issues': [
            {
                'code': 'WCAG2AA.Principle1.Guideline1_1.1_1_1.H37',
                'type': 'error',
                'message': 'Img element missing an alt attribute',
                'selector': 'img.hero-image',
                'context': '<img class="hero-image" src="...">'
            },
            {
                'code': 'WCAG2AA.Principle1.Guideline1_4.1_4_3.G18',
                'type': 'warning',
                'message': 'Contrast ratio is insufficient',
                'selector': 'p.subtle-text',
                'context': '<p class="subtle-text">...'
            }
        ],
        'checkedAt': datetime.utcnow().isoformat(),
        'checkedBy': SERVICE_ID
    }

    return jsonify(results)


@app.route('/api/v1/batch-check', methods=['POST'])
@require_service_auth
def batch_check_accessibility():
    """
    Check multiple pages for accessibility issues.
    Requires valid service JWT.
    """
    data = request.get_json()

    if not data or 'urls' not in data:
        return jsonify({'error': 'urls array is required'}), 400

    urls = data['urls']
    standard = data.get('standard', 'WCAG2AA')

    results = []
    total_issues = 0

    for url in urls[:10]:  # Limit to 10 URLs per batch
        # Simulate check for each URL
        result = {
            'url': url,
            'score': 90,
            'issueCount': 2
        }
        results.append(result)
        total_issues += result['issueCount']

    return jsonify({
        'results': results,
        'totalUrls': len(results),
        'totalIssues': total_issues,
        'standard': standard
    })


# ============================================================================
# Alternative: Callback validation to dotCMS
# ============================================================================

def validate_token_via_dotcms(token: str, dotcms_url: str) -> dict:
    """
    Alternative validation method: Call back to dotCMS to validate the token.

    Use this approach if:
    - You can't share the signing key with the microservice
    - You want centralized token revocation
    - You need additional validation beyond JWT verification

    Args:
        token: The JWT token to validate
        dotcms_url: Base URL of dotCMS (e.g., "https://dotcms.example.com")

    Returns:
        Validated claims dict if valid

    Raises:
        Exception if validation fails
    """
    import requests

    response = requests.post(
        f"{dotcms_url}/api/v1/service-auth/validate",
        json={
            'token': token,
            'expectedAudience': SERVICE_ID
        },
        timeout=5
    )

    if response.status_code == 200:
        data = response.json()
        if data.get('entity', {}).get('valid'):
            return data['entity']['claims']

    raise Exception(f"Token validation failed: {response.text}")


# ============================================================================
# Main
# ============================================================================

if __name__ == '__main__':
    print(f"Starting {SERVICE_ID} service...")
    print(f"JWT validation enabled with shared key")

    # In production, use gunicorn or similar
    app.run(host='0.0.0.0', port=8080, debug=False)
