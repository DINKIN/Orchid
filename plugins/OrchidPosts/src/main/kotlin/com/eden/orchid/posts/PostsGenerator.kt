package com.eden.orchid.posts

import com.eden.common.util.EdenUtils
import com.eden.orchid.api.OrchidContext
import com.eden.orchid.api.generators.FolderCollection
import com.eden.orchid.api.generators.OrchidCollection
import com.eden.orchid.api.generators.OrchidGenerator
import com.eden.orchid.api.options.annotations.Description
import com.eden.orchid.api.options.annotations.ListClass
import com.eden.orchid.api.options.annotations.Option
import com.eden.orchid.api.options.annotations.StringDefault
import com.eden.orchid.api.theme.pages.OrchidPage
import com.eden.orchid.posts.model.Author
import com.eden.orchid.posts.model.CategoryModel
import com.eden.orchid.posts.model.PostsModel
import com.eden.orchid.posts.pages.PostPage
import com.eden.orchid.posts.permalink.PostsPermalinkStrategy
import com.eden.orchid.posts.utils.PostsUtils
import com.eden.orchid.utilities.dashCase
import com.eden.orchid.utilities.from
import com.eden.orchid.utilities.to
import com.eden.orchid.utilities.words
import java.util.*
import java.util.regex.Pattern
import java.util.stream.Stream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Description("Share your thoughts and interests with blog posts and archives.")
class PostsGenerator @Inject
constructor(context: OrchidContext, val permalinkStrategy: PostsPermalinkStrategy, val postsModel: PostsModel)
    : OrchidGenerator(context, "posts", OrchidGenerator.PRIORITY_EARLY) {

    companion object {
        val pageTitleRegex = Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})-([\\w-]+)")
    }

    @Option
    @StringDefault(":category/:year/:month/:day/:slug")
    lateinit var permalink: String

    @Option
    @StringDefault("<!--more-->")
    lateinit var excerptSeparator: String

    @Option
    @ListClass(Author::class)
    var authors: List<Author> = emptyList()

    @Option
    var categories: Array<String> = emptyArray()

    @Option
    @StringDefault("posts")
    lateinit var baseDir: String

    override fun startIndexing(): List<OrchidPage> {
        postsModel.initialize(permalink, layout, excerptSeparator, authors)

        if (EdenUtils.isEmpty(categories)) {
            val posts = getPostsList(null)
            postsModel.categories.put(null, CategoryModel(null, posts))
        } else {
            for (category in categories) {
                val posts = getPostsList(category)
                postsModel.categories.put(category, CategoryModel(category, posts))
            }
        }

        val allPages = ArrayList<OrchidPage>()
        for (key in postsModel.categories.keys) {
            allPages.addAll(postsModel.categories[key]!!.first)
        }

        return allPages
    }

    override fun startGeneration(posts: Stream<out OrchidPage>) {
        posts.forEach({ context.renderTemplate(it) })
    }

    private fun previous(posts: List<OrchidPage>, i: Int): OrchidPage? {
        if (posts.size > 1) {
            if (i != 0) {
                return posts[i - 1]
            }
        }

        return null
    }

    private fun next(posts: List<OrchidPage>, i: Int): OrchidPage? {
        if (posts.size > 1) {
            if (i < posts.size - 1) {
                return posts[i + 1]
            }
        }

        return null
    }

    private fun getPostsList(category: String?): MutableList<PostPage> {
        val baseCategoryPath = if (EdenUtils.isEmpty(category)) baseDir else baseDir + "/" + category
        val resourcesList = context.getLocalResourceEntries(baseCategoryPath, null, true)

        val posts = ArrayList<PostPage>()

        for (entry in resourcesList) {
            val formattedFilename = PostsUtils.getPostFilename(entry, baseCategoryPath)
            val matcher = pageTitleRegex.matcher(formattedFilename)

            if (matcher.matches()) {
                val post = PostPage(entry, postsModel, category)

                post.year = Integer.parseInt(matcher.group(1))
                post.month = Integer.parseInt(matcher.group(2))
                post.day = Integer.parseInt(matcher.group(3))
                post.title = matcher.group(4).from { dashCase() }.to { words { capitalize() } }

                val permalink = if (!EdenUtils.isEmpty(post.permalink)) post.permalink else postsModel.permalink
                permalinkStrategy.applyPermalink(post, permalink)
                posts.add(post)
            }
        }

        posts.sortWith(PostsModel.postPageComparator)
        posts.mapIndexed { i, post ->
            if (next(posts, i) != null) {
                post.previous = next(posts, i)
            }
            if (previous(posts, i) != null) {
                post.next = previous(posts, i)
            }
        }

        return posts
    }

    override fun getCollections(): List<OrchidCollection<*>> {
        val collectionsList = ArrayList<OrchidCollection<*>>()

        categories.forEach {
            val baseCategoryPath = if (EdenUtils.isEmpty(it)) baseDir else baseDir + "/" + it

            val collection = FolderCollection(
                    this,
                    it,
                    postsModel.categories[it]?.first,
                    PostPage::class.java,
                    baseCategoryPath
            )
            collection.label = "Blog - $it"

            collectionsList.add(collection)
        }

        return collectionsList
    }
}

