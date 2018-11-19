//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples

import com.pdftron.android.pdfnetsdksamples.OutputListener
import com.pdftron.android.pdfnetsdksamples.PDFNetSample
import com.pdftron.android.pdfnetsdksamples.R
import com.pdftron.android.pdfnetsdksamples.util.Utils
import com.pdftron.common.PDFNetException
import com.pdftron.pdf.Action
import com.pdftron.pdf.Bookmark
import com.pdftron.pdf.Destination
import com.pdftron.pdf.FileSpec
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.Page
import com.pdftron.sdf.Obj
import com.pdftron.sdf.SDFDoc

import java.io.File
import java.util.ArrayList

class BookmarkTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_bookmarks_title)
        setDescription(R.string.sample_bookmarks_description)
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)

        // The following example illustrates how to create and edit the outline tree
        // using high-level Bookmark methods.
        try {
            mOutputListener!!.println("Opening the input file...")

            val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "numbered.pdf")!!.absolutePath)
            doc.initSecurityHandler()

            // Lets first create the root bookmark items.
            val red = Bookmark.create(doc, "Red")
            val green = Bookmark.create(doc, "Green")
            val blue = Bookmark.create(doc, "Blue")

            doc.addRootBookmark(red)
            doc.addRootBookmark(green)
            doc.addRootBookmark(blue)

            // You can also add new root bookmarks using Bookmark.AddNext("...")
            blue.addNext("foo")
            blue.addNext("bar")

            // We can now associate new bookmarks with page destinations:

            // The following example creates an 'explicit' destination (see
            // section '8.2.1 Destinations' in PDF Reference for more details)
            val red_dest = Destination.createFit(doc.pageIterator.next())
            red.action = Action.createGoto(red_dest)

            // Create an explicit destination to the first green page in the document
            green.action = Action.createGoto(
                    Destination.createFit(doc.getPage(10)))

            // The following example creates a 'named' destination (see
            // section '8.2.1 Destinations' in PDF Reference for more details)
            // Named destinations have certain advantages over explicit destinations.
            val key = byteArrayOf('b'.toByte(), 'l'.toByte(), 'u'.toByte(), 'e'.toByte(), '1'.toByte())
            val blue_action = Action.createGoto(key,
                    Destination.createFit(doc.getPage(19)))

            blue.action = blue_action

            // We can now add children Bookmarks
            val sub_red1 = red.addChild("Red - Page 1")
            sub_red1.action = Action.createGoto(Destination.createFit(doc.getPage(1)))
            val sub_red2 = red.addChild("Red - Page 2")
            sub_red2.action = Action.createGoto(Destination.createFit(doc.getPage(2)))
            val sub_red3 = red.addChild("Red - Page 3")
            sub_red3.action = Action.createGoto(Destination.createFit(doc.getPage(3)))
            val sub_red4 = sub_red3.addChild("Red - Page 4")
            sub_red4.action = Action.createGoto(Destination.createFit(doc.getPage(4)))
            val sub_red5 = sub_red3.addChild("Red - Page 5")
            sub_red5.action = Action.createGoto(Destination.createFit(doc.getPage(5)))
            val sub_red6 = sub_red3.addChild("Red - Page 6")
            sub_red6.action = Action.createGoto(Destination.createFit(doc.getPage(6)))

            // Example of how to find and delete a bookmark by title text.
            val foo = doc.firstBookmark.find("foo")
            if (foo.isValid) {
                foo.delete()
            } else {
                throw Exception("Foo is not Valid")
            }

            val bar = doc.firstBookmark.find("bar")
            if (bar.isValid) {
                bar.delete()
            } else {
                throw Exception("Bar is not Valid")
            }

            // Adding color to Bookmarks. Color and other formatting can help readers
            // get around more easily in large PDF documents.
            red.setColor(1.0, 0.0, 0.0)
            green.setColor(0.0, 1.0, 0.0)
            green.flags = 2            // set bold font
            blue.setColor(0.0, 0.0, 1.0)
            blue.flags = 3            // set bold and itallic

            doc.save(Utils.createExternalFile("bookmark.pdf").absolutePath, SDFDoc.SaveMode.NO_FLAGS, null)
            mFileList.add(File(doc.fileName).name)
            doc.close()
            mOutputListener!!.println("Done. Result saved in bookmark.pdf")
        } catch (e: Exception) {
            mOutputListener!!.println(e.stackTrace)
        }

        // The following example illustrates how to traverse the outline tree using
        // Bookmark navigation methods: Bookmark.GetNext(), Bookmark.GetPrev(),
        // Bookmark.GetFirstChild () and Bookmark.GetLastChild ().
        try {
            // Open the document that was saved in the previous code sample
            val doc = PDFDoc(Utils.createExternalFile("bookmark.pdf").absolutePath)
            doc.initSecurityHandler()

            val root = doc.firstBookmark
            PrintOutlineTree(root)

            doc.close()
            mOutputListener!!.println("Done.")
        } catch (e: Exception) {
            mOutputListener!!.println(e.stackTrace)
        }

        // The following example illustrates how to create a Bookmark to a page
        // in a remote document. A remote go-to action is similar to an ordinary
        // go-to action, but jumps to a destination in another PDF file instead
        // of the current file. See Section 8.5.3 'Remote Go-To Actions' in PDF
        // Reference Manual for details.
        try {
            // Open the document that was saved in the previous code sample
            val doc = PDFDoc(Utils.createExternalFile("bookmark.pdf").absolutePath)
            doc.initSecurityHandler()

            // Create file specification (the file reffered to by the remote bookmark)
            val file_spec = doc.createIndirectDict()
            file_spec.putName("Type", "Filespec")
            file_spec.putString("F", "bookmark.pdf")
            val spec = FileSpec(file_spec)
            val goto_remote = Action.createGotoRemote(spec, 5, true)

            val remoteBookmark1 = Bookmark.create(doc, "REMOTE BOOKMARK 1")
            remoteBookmark1.action = goto_remote
            doc.addRootBookmark(remoteBookmark1)

            // Create another remote bootmark, but this time using the low-level SDF/Cos API.
            // Create a remote action
            val remoteBookmark2 = Bookmark.create(doc, "REMOTE BOOKMARK 2")
            doc.addRootBookmark(remoteBookmark2)

            val gotoR = remoteBookmark2.sdfObj.putDict("A")
            run {
                gotoR.putName("S", "GoToR") // Set action type
                gotoR.putBool("NewWindow", true)

                // Set the file specification
                gotoR.put("F", file_spec)

                // jump to the first page. Note that pages are indexed from 0.
                val dest = gotoR.putArray("D") // Set the destination
                dest.pushBackNumber(9.0)
                dest.pushBackName("Fit")
            }

            doc.save(Utils.createExternalFile("bookmark_remote.pdf").absolutePath, SDFDoc.SaveMode.LINEARIZED, null)
            mFileList.add(File(doc.fileName).name)
            doc.close()
            mOutputListener!!.println("Done. Result saved in bookmark_remote.pdf")
        } catch (e: Exception) {
            mOutputListener!!.println(e.stackTrace)
        }

        for (file in mFileList) {
            addToFileList(file)
        }
        printFooter(outputListener)
    }

    companion object {

        private var mOutputListener: OutputListener? = null

        private val mFileList = ArrayList<String>()

        @Throws(PDFNetException::class)
        internal fun PrintIndent(item: Bookmark) {
            val ident = item.indent - 1
            for (i in 0 until ident) mOutputListener!!.print("  ")
        }

        // Prints out the outline tree to the standard output
        @Throws(PDFNetException::class)
        internal fun PrintOutlineTree(item: Bookmark) {
            var item = item
            while (item.isValid) {
                PrintIndent(item)
                mOutputListener!!.print((if (item.isOpen) "- " else "+ ") + item.title + " ACTION -> ")

                // Print Action
                val action = item.action
                if (action.isValid) {
                    if (action.type == Action.e_GoTo) {
                        val dest = action.dest
                        if (dest.isValid) {
                            val page = dest.page
                            mOutputListener!!.println("GoTo Page #" + page.index)
                        }
                    } else {
                        mOutputListener!!.println("Not a 'GoTo' action")
                    }
                } else {
                    mOutputListener!!.println("NULL")
                }

                if (item.hasChildren())
                // Recursively print children sub-trees
                {
                    PrintOutlineTree(item.firstChild)
                }
                item = item.next
            }
        }
    }

}