# -*- coding: utf-8 -*-
import bcrypt

h = "$2a$10$0585LWByUEnx8KXANYpcGuQKrfv0bJ6Z2uitphjWe3i/UkylhT07G"
ok = bcrypt.checkpw(b"Btr163#2026", h.encode("utf-8"))
print("match:", ok)
