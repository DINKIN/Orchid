package com.eden.orchid.taxonomies.permalink.pathtype

import com.eden.orchid.api.theme.pages.OrchidPage
import com.eden.orchid.api.theme.permalinks.PermalinkPathType
import com.eden.orchid.taxonomies.pages.TaxonomyArchivePage
import com.eden.orchid.taxonomies.pages.TermArchivePage
import javax.inject.Inject

class TaxonomyPathType
@Inject
constructor() : PermalinkPathType(100) {

    override fun acceptsKey(page: OrchidPage, key: String): Boolean {
        return key == "taxonomy" && (page is TaxonomyArchivePage || page is TermArchivePage)
    }

    override fun format(page: OrchidPage, key: String): String? {
        return if (page is TaxonomyArchivePage) page.taxonomy.key
        else if (page is TermArchivePage) page.taxonomy.key
        else null
    }

}

