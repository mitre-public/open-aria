# Changes from the Original

`OpenARIA` is an open-source edition of the _Aviation Risk Identification and Assessment_ (ARIA) software
program developed by MITRE on behalf of the Federal Aviation Administration's (FAA) Safety and Technical Training (AJI)
Service Unit.


## Reason for changes

`OpenARIA` (i.e., this code-base) differs from the original edition of ARIA when:
- A section of code was deemed _"not in-scope for the initial open sourcing"_.
- A section of code was deemed _"too FAA specific"_.
- A section of code was deemed _"too inconvenient for new adopters"_.


## Synopsis of changes

Here is a synopsis of the difference between `OpenARIA` and the original edition of ARIA deployed at the FAA on Oct 1st 2021.  

- Removed Surface ARIA, the module dedicated to finding _events on & near airport surface._
- Removed CFIT ARIA, the module dedicated to finding _terrain & obstacle proximity events._
- Removed integrations with the FAA's internal encounter tracking and reporting systems.
- Refactored when _should publish_ logic and _which airspace_ logic are integrated. These choices are no longer "baked in"
  to all Airborne Events. This work is now controlled via an injectable Strategy object.
- Refactored how formation flights are filtered out of results sets.
- Refactored the code base to improved support for different data feeds. 