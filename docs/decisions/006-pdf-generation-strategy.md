# PDF generation
Use Android PdfDocument: no additional PDF library or watermark. One image decoded
at a time with bounded sampling, fit on portrait/landscape A4 pages in user order.
Standard/high image resolution can be selected. PdfDocument itself retains native
page drawing data until write; exports are capped at 50 pages. No searchable PDF
text layer is claimed. Unreadable pages fail explicitly rather than silently disappear.
