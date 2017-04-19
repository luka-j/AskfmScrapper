# AskfmScrapper
Nothing is private, especially not publicly published data. Just a tool I wrote in spare time.

Give it a username, and it will download all questions/answers/likes/authors in a file. Convinient for `grep`. Read comments for more detailed instructions. Code is awful (at some spots less, at some, especially parts of Retriever, more), mostly because I was just trying to finish it quickly. It works (usually). Or, rather, it used to work, now it will give "No robots allowed" after a couple of accounts.


Known issues:
- Doesn't work with videos
- Single-threaded. I don't own a high-end PC, and I like to do something else while scrapping random strangers' data.
- Might fail to record a question or two. Very minor, however
- Might fail to properly format question/answer, resulting in text artifacts
- Letters in non-English alphabet won't be encoded properly in filenames on Windows
- Might fail to work in many other ways at any moment, but that doesn't happen. Okay, very rarely, for very edge-y cases.


## How it works
It uses the same endpoint "show more" button would trigger, but leaving time parameter empty. It then proceeds to parse server-side rendered string of the page, pulling out the data and saving it in a separate file. It puts any links to other askfm accounts found in a queue to be downloaded next. It exploits the fact that endpoint was exempt from rate limiting, allowing practically infinite requests.

## Current status
Doesn't work. Ask.fm went through a redesign and I apploud them for it. I see no practical purpose to improve on this, but you're welcome to pick it up if you like.

Just something I've written in my spare time for fun. It could have been much better organized and optimized, but I saw no purpose in bothering with it.
