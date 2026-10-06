const categoryFilter =
        document.getElementById(
                "work-category-filter"
        );

if (categoryFilter) {

    categoryFilter.addEventListener(
            "change",
            () => {

                const url =
                        new URL(
                                window.location.pathname,
                                window.location.origin
                        );

                const category =
                        categoryFilter.value;

                if (category !== "") {
                    url.searchParams.set(
                            "category",
                            category
                    );
                }

                window.location.href =
                        url.pathname
                        + url.search;
            }
    );
}