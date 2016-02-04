#!/usr/bin/env python
import sys
import imgix


def _sign_url(domain, key, image, options):
    builder = imgix.UrlBuilder(domain, sign_key=key)
    return builder.create_url(image, options)

if __name__ == '__main__':
    if len(sys.argv) >= 4:
        domain, sign_key, image, options = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4:]

        options_dict = {}

        for option in options:
            k, v = option.split('=')
            options_dict[k] = v

        print _sign_url(domain, sign_key, image, options_dict)

    else:
        print 'Usage: ./sign_image.py <domain> <key> <image>'
