
import re
import json

from extractor import (
    _parse_markdown_response, 
    _safe_float, 
    _merge_pages
)

# Test _safe_float for currency formats
def test_currency_formatting():
    assert _safe_float("₹24,500") == 24500.0
    assert _safe_float("INR 24,500") == 24500.0
    assert _safe_float("24,500.00") == 24500.0
    assert _safe_float("₹ 24,500.00") == 24500.0

def test_markdown_fallback_alliance_air():
    text = """
    Basic Fare: 15918
    GST: 770
    Aviation security fee: 1652
    Cute Fee: 735
    User Development Fee: 5425
    Total Fare: 24500
    """
    res = _parse_markdown_response(text, 1)
    assert res is not None
    assert res["fare"] is not None
    assert res["fare"]["base_fare_total"] == 15918.0
    assert res["fare"]["total_amount"] == 24500.0

def test_markdown_fallback_basic_fare_only():
    text = "Basic Fare: 5000"
    res = _parse_markdown_response(text, 1)
    assert res["fare"]["base_fare_total"] == 5000.0
    assert "total_amount" not in res["fare"]

def test_markdown_fallback_basic_plus_total():
    text = """
    Basic Fare: 5000
    GST: 900
    Total Fare: 5900
    """
    res = _parse_markdown_response(text, 1)
    assert res["fare"]["base_fare_total"] == 5000.0
    assert res["fare"]["total_amount"] == 5900.0

def test_markdown_fallback_total_amount_term():
    text = """
    Base Fare: 5000
    Taxes: 900
    Total Amount: 5900
    """
    res = _parse_markdown_response(text, 1)
    assert res["fare"]["base_fare_total"] == 5000.0
    assert res["fare"]["total_amount"] == 5900.0

def test_markdown_fallback_grand_total_term():
    text = """
    Basic Fare: 5000
    Taxes: 900
    Grand Total: 5900
    """
    res = _parse_markdown_response(text, 1)
    assert res["fare"]["base_fare_total"] == 5000.0
    assert res["fare"]["total_amount"] == 5900.0

def test_merge_pages_leaves_total_null():
    page_results = [{
        "page_type": "PASSENGER",
        "passengers": [{"passenger_name": "JOHN DOE"}],
        "fare": {"base_fare_total": 5000} # no total_amount
    }]
    merged = _merge_pages(page_results)
    assert len(merged) == 1
    assert merged[0]["base_fare"] == 5000.0
    assert merged[0]["total_amount"] is None

if __name__ == '__main__':
    test_currency_formatting()
    test_markdown_fallback_alliance_air()
    test_markdown_fallback_basic_fare_only()
    test_markdown_fallback_basic_plus_total()
    test_markdown_fallback_total_amount_term()
    test_markdown_fallback_grand_total_term()
    test_merge_pages_leaves_total_null()
    print("All tests passed.")
