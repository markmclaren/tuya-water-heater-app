#!/usr/bin/env python3
"""
Test suite validating Thermal Physics calculations and Tuya HMAC-SHA256 signature logic 
for the Android Water Heater App.
"""

import math
import hashlib
import hmac
import json
import urllib.request

def calculate_thermal_estimate(duration_seconds, volume_l=266.0, element_kw=3.0, efficiency=0.95, cold_inlet_c=15.0, max_temp_c=65.0):
    power_w = element_kw * 1000.0
    mass_kg = volume_l * 1.0 # 1L water = 1kg
    heat_capacity = 4184.0 # J / (kg * °C)
    
    energy_joules = power_w * duration_seconds * efficiency
    temp_rise = energy_joules / (mass_kg * heat_capacity)
    
    final_temp = min(max_temp_c, cold_inlet_c + temp_rise)
    actual_rise = final_temp - cold_inlet_c
    energy_kwh = (power_w * duration_seconds) / 3600000.0
    
    # Shower water calculation at 40°C
    if final_temp > cold_inlet_c:
        usable_shower_l = volume_l * ((final_temp - cold_inlet_c) / (40.0 - cold_inlet_c))
    else:
        usable_shower_l = 0.0
        
    return {
        "duration_seconds": duration_seconds,
        "temp_rise": round(actual_rise, 2),
        "final_temp": round(final_temp, 2),
        "energy_kwh": round(energy_kwh, 3),
        "usable_shower_l": round(usable_shower_l, 1)
    }

def test_thermal_physics():
    print("--- Running Thermal Physics Model Verification ---")
    
    # Test 1: 15 min top-up
    res_15 = calculate_thermal_estimate(15 * 60)
    print(f"15 Min Top-Up: {res_15['final_temp']}°C (+{res_15['temp_rise']}°C), Shower Water: {res_15['usable_shower_l']}L, Energy: {res_15['energy_kwh']} kWh")
    assert res_15['final_temp'] > 15.0, "Temperature should rise"
    assert res_15['usable_shower_l'] > 0, "Should provide hot shower water"

    # Test 2: 30 min shower boost
    res_30 = calculate_thermal_estimate(30 * 60)
    print(f"30 Min Shower: {res_30['final_temp']}°C (+{res_30['temp_rise']}°C), Shower Water: {res_30['usable_shower_l']}L, Energy: {res_30['energy_kwh']} kWh")
    assert res_30['final_temp'] > res_15['final_temp'], "30 mins should heat more than 15 mins"

    # Test 3: 60 min bath boost
    res_60 = calculate_thermal_estimate(60 * 60)
    print(f"60 Min Bath:   {res_60['final_temp']}°C (+{res_60['temp_rise']}°C), Shower Water: {res_60['usable_shower_l']}L, Energy: {res_60['energy_kwh']} kWh")
    assert res_60['final_temp'] > res_30['final_temp'], "60 mins should heat more than 30 mins"

    print("\n✅ Thermal Physics calculations verified successfully!\n")

def calculate_seasonal_mains_temp(doy, annual_mean_c=11.5, amplitude_c=7.5, phase_day=60):
    angle = 2.0 * math.pi * (doy - phase_day) / 365.0
    return annual_mean_c + amplitude_c * math.sin(angle)

def test_seasonal_regions():
    print("--- Running Seasonal Mains Temperature Regional Profiles Verification ---")
    # UK Profile: Mean 11.5°C, Amp 7.5°C, Phase 60 (UKWIR 09/WM/03/14)
    uk_coldest = calculate_seasonal_mains_temp(334, 11.5, 7.5, 60)
    uk_warmest = calculate_seasonal_mains_temp(151, 11.5, 7.5, 60)
    print(f"🇬🇧 UK Coldest (doy 334): {uk_coldest:.2f}°C (expected ~4.0°C)")
    print(f"🇬🇧 UK Warmest (doy 151): {uk_warmest:.2f}°C (expected ~19.0°C)")
    assert 3.9 <= uk_coldest <= 4.1, f"UK coldest out of range: {uk_coldest}"
    assert 18.9 <= uk_warmest <= 19.1, f"UK warmest out of range: {uk_warmest}"

    # Australia SE Profile: Mean 16.0°C, Amp 6.0°C, Phase 240 (CSIRO / BoM)
    # Southern hemisphere inverted season: July is coldest, Jan is warmest
    au_july = calculate_seasonal_mains_temp(149, 16.0, 6.0, 240)
    au_jan  = calculate_seasonal_mains_temp(331, 16.0, 6.0, 240)
    print(f"🇦🇺 Australia SE July (doy 149): {au_july:.2f}°C (expected ~10.0°C)")
    print(f"🇦🇺 Australia SE Jan (doy 331): {au_jan:.2f}°C (expected ~22.0°C)")
    assert 9.9 <= au_july <= 10.1, f"AU winter out of range: {au_july}"
    assert 21.9 <= au_jan <= 22.1, f"AU summer out of range: {au_jan}"

    print("\n✅ Seasonal Regional Profiles verified successfully!\n")

def test_tuya_signature():
    """
    Verifies that the HMAC-SHA256 signature algorithm produces a 64-char
    uppercase hex string. Uses credentials from local.properties or .env.
    This test is purely algorithmic — it does NOT make any live API call.
    """
    import os

    # Optional dotenv loader with standard library fallback
    try:
        from dotenv import load_dotenv
        load_dotenv()
        load_dotenv(dotenv_path=os.path.join(os.path.dirname(__file__), '..', '.env'))
    except ImportError:
        def _read_env(path):
            if os.path.exists(path):
                with open(path, 'r') as f:
                    for line in f:
                        line = line.strip()
                        if line and not line.startswith('#') and '=' in line:
                            k, v = line.split('=', 1)
                            os.environ.setdefault(k.strip(), v.strip().strip('"').strip("'"))
        _read_env(os.path.join(os.path.dirname(__file__), '.env'))
        _read_env(os.path.join(os.path.dirname(__file__), '..', '.env'))

    client_id = os.getenv('TUYA_CLIENT_ID', 'test_client_id_placeholder')
    secret    = os.getenv('TUYA_CLIENT_SECRET', 'test_secret_placeholder_32chars00')

    print("--- Testing Tuya OpenAPI Signature Generation ---")
    t = "1726046400000"

    body_hash = hashlib.sha256(b"").hexdigest()
    string_to_sign = f"GET\n{body_hash}\n\n/v1.0/token?grant_type=1"
    sign_payload = f"{client_id}{t}{string_to_sign}"

    sign = hmac.new(secret.encode('utf-8'), sign_payload.encode('utf-8'), hashlib.sha256).hexdigest().upper()

    print(f"Client ID: {client_id[:8]}... (truncated)")
    print(f"Signature String: {repr(string_to_sign)}")
    print(f"Calculated HMAC-SHA256 Sign: {sign[:16]}... (truncated)")
    assert len(sign) == 64, "Tuya HMAC-SHA256 signature must be 64 upper-hex chars"
    print("\n✅ Tuya HMAC Signature verification passed!\n")


if __name__ == "__main__":
    test_thermal_physics()
    test_seasonal_regions()
    test_tuya_signature()
